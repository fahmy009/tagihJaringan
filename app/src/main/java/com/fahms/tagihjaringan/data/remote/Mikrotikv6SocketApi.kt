package com.fahms.tagihjaringan.data.remote

import com.fahms.tagihjaringan.data.model.MikrotikActiveSession
import com.fahms.tagihjaringan.data.model.MikrotikSecret
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.security.MessageDigest

class Mikrotikv6SocketApi {

    suspend fun getSecrets(
        host: String,
        port: Int,
        user: String,
        pass: String,
        profileFilter: String? = null
    ): List<MikrotikSecret> {
        return withContext(Dispatchers.IO) {
            val list = mutableListOf<MikrotikSecret>()
            executeSocketCommand(host, port, user, pass) { out, input ->
                writeSentence(out, listOf("/ppp/secret/print"))
                val sentences = readResponseSentences(input)
                sentences.forEach { words ->
                    if (words.firstOrNull() == "!re") {
                        val map = parseWordMap(words)
                        val name = map["name"] ?: ""
                        val profile = map["profile"] ?: ""
                        val comment = map["comment"] ?: ""
                        val id = map[".id"] ?: name
                        val disabled = map["disabled"] ?: "false"
                        if (name.isNotBlank()) {
                            list.add(
                                MikrotikSecret(
                                    id = id,
                                    name = name,
                                    profile = profile,
                                    comment = comment,
                                    disabled = disabled
                                )
                            )
                        }
                    }
                }
            }
            if (!profileFilter.isNullOrBlank()) {
                val allowed = profileFilter.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }
                list.filter { secret -> allowed.any { p -> (secret.profile ?: "").trim().lowercase() == p } }
            } else {
                list
            }
        }
    }

    suspend fun patchSecret(
        host: String,
        port: Int,
        user: String,
        pass: String,
        secretIdOrName: String,
        disabled: Boolean
    ): Boolean {
        return withContext(Dispatchers.IO) {
            var success = false
            executeSocketCommand(host, port, user, pass) { out, input ->
                val cmd = listOf(
                    "/ppp/secret/set",
                    "=.id=$secretIdOrName",
                    "=disabled=${if (disabled) "yes" else "no"}"
                )
                writeSentence(out, cmd)
                val sentences = readResponseSentences(input)
                success = sentences.any { it.firstOrNull() == "!done" }
            }
            success
        }
    }

    suspend fun getActiveSessions(
        host: String,
        port: Int,
        user: String,
        pass: String,
        nameFilter: String? = null
    ): List<MikrotikActiveSession> {
        return withContext(Dispatchers.IO) {
            val list = mutableListOf<MikrotikActiveSession>()
            executeSocketCommand(host, port, user, pass) { out, input ->
                val cmd = mutableListOf("/ppp/active/print")
                if (!nameFilter.isNullOrBlank()) {
                    cmd.add("?name=$nameFilter")
                }
                writeSentence(out, cmd)
                val sentences = readResponseSentences(input)
                sentences.forEach { words ->
                    if (words.firstOrNull() == "!re") {
                        val map = parseWordMap(words)
                        val name = map["name"] ?: ""
                        val address = map["address"] ?: ""
                        val id = map[".id"] ?: name
                        if (name.isNotBlank()) {
                            list.add(
                                MikrotikActiveSession(
                                    id = id,
                                    name = name,
                                    address = address
                                )
                            )
                        }
                    }
                }
            }
            list
        }
    }

    suspend fun deleteActiveSession(
        host: String,
        port: Int,
        user: String,
        pass: String,
        activeId: String
    ): Boolean {
        return withContext(Dispatchers.IO) {
            var success = false
            executeSocketCommand(host, port, user, pass) { out, input ->
                val cmd = listOf(
                    "/ppp/active/remove",
                    "=.id=$activeId"
                )
                writeSentence(out, cmd)
                val sentences = readResponseSentences(input)
                success = sentences.any { it.firstOrNull() == "!done" }
            }
            success
        }
    }

    // Universal Dual-Login TCP Socket Execution Helper (v6.43+ Direct & Legacy <v6.43 MD5)
    private fun executeSocketCommand(
        host: String,
        port: Int,
        user: String,
        pass: String,
        block: (OutputStream, InputStream) -> Unit
    ) {
        var socket: Socket? = null
        try {
            val cleanHost = host.replace("http://", "").replace("https://", "").split(":")[0].trim()
            val cleanPort = if (port > 0) port else 8728
            socket = Socket()
            socket.connect(InetSocketAddress(cleanHost, cleanPort), 8000)
            socket.soTimeout = 8000

            val out = socket.getOutputStream()
            val input = socket.getInputStream()

            // 1. Try Direct Login (RouterOS v6.43+)
            writeSentence(out, listOf("/login", "=name=$user", "=password=$pass"))
            val sentences1 = readResponseSentences(input)
            val firstSentence = sentences1.find { it.firstOrNull() == "!done" || it.firstOrNull() == "!trap" }

            var loginOk = false

            if (firstSentence != null && firstSentence.firstOrNull() == "!done") {
                val map1 = parseWordMap(firstSentence)
                val challengeHex = map1["ret"]
                if (challengeHex.isNullOrBlank()) {
                    // Direct login succeeded on RouterOS v6.43+
                    loginOk = true
                } else {
                    // Legacy MD5 Challenge required (RouterOS <6.43)
                    val responseHash = calculateMd5ChallengeResponse(pass, challengeHex)
                    writeSentence(out, listOf("/login", "=name=$user", "=response=00$responseHash"))
                    val sentences2 = readResponseSentences(input)
                    loginOk = sentences2.any { it.firstOrNull() == "!done" }
                }
            } else if (firstSentence == null) {
                // Legacy fallback: Send empty /login first
                writeSentence(out, listOf("/login"))
                val sentencesFallback = readResponseSentences(input)
                val challengeSentence = sentencesFallback.find { it.firstOrNull() == "!done" }
                if (challengeSentence != null) {
                    val mapFB = parseWordMap(challengeSentence)
                    val challengeHex = mapFB["ret"]
                    if (!challengeHex.isNullOrBlank()) {
                        val responseHash = calculateMd5ChallengeResponse(pass, challengeHex)
                        writeSentence(out, listOf("/login", "=name=$user", "=response=00$responseHash"))
                        val sentences2 = readResponseSentences(input)
                        loginOk = sentences2.any { it.firstOrNull() == "!done" }
                    }
                }
            }

            if (loginOk) {
                block(out, input)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                socket?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun calculateMd5ChallengeResponse(pass: String, challengeHex: String): String {
        val md = MessageDigest.getInstance("MD5")
        md.update(0)
        md.update(pass.toByteArray(Charsets.UTF_8))
        md.update(hexStringToByteArray(challengeHex))
        val digest = md.digest()
        val sb = StringBuilder()
        for (b in digest) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }

    private fun hexStringToByteArray(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    private fun writeLength(out: OutputStream, length: Int) {
        if (length < 0x80) {
            out.write(length)
        } else if (length < 0x4000) {
            out.write((length shr 8 or 0x80))
            out.write(length and 0xFF)
        } else if (length < 0x200000) {
            out.write((length shr 16 or 0xC0))
            out.write((length shr 8) and 0xFF)
            out.write(length and 0xFF)
        } else if (length < 0x10000000) {
            out.write((length shr 24 or 0xE0))
            out.write((length shr 16) and 0xFF)
            out.write((length shr 8) and 0xFF)
            out.write(length and 0xFF)
        } else {
            out.write(0xF0)
            out.write((length shr 24) and 0xFF)
            out.write((length shr 16) and 0xFF)
            out.write((length shr 8) and 0xFF)
            out.write(length and 0xFF)
        }
    }

    private fun writeWord(out: OutputStream, word: String) {
        val bytes = word.toByteArray(Charsets.UTF_8)
        writeLength(out, bytes.size)
        out.write(bytes)
    }

    private fun writeSentence(out: OutputStream, words: List<String>) {
        for (word in words) {
            writeWord(out, word)
        }
        out.write(0) // Sentence terminator
        out.flush()
    }

    private fun readLength(input: InputStream): Int {
        val b1 = input.read()
        if (b1 == -1) return -1
        if ((b1 and 0x80) == 0) {
            return b1
        }
        if ((b1 and 0xC0) == 0x80) {
            val b2 = input.read()
            return ((b1 and 0x3F) shl 8) or b2
        }
        if ((b1 and 0xE0) == 0xC0) {
            val b2 = input.read()
            val b3 = input.read()
            return ((b1 and 0x1F) shl 16) or (b2 shl 8) or b3
        }
        if ((b1 and 0xF0) == 0xE0) {
            val b2 = input.read()
            val b3 = input.read()
            val b4 = input.read()
            return ((b1 and 0x0F) shl 24) or (b2 shl 16) or (b3 shl 8) or b4
        }
        val b2 = input.read()
        val b3 = input.read()
        val b4 = input.read()
        val b5 = input.read()
        return (b2 shl 24) or (b3 shl 16) or (b4 shl 8) or b5
    }

    private fun readWord(input: InputStream): String? {
        val len = readLength(input)
        if (len <= 0) return if (len == 0) "" else null
        val bytes = ByteArray(len)
        var readTotal = 0
        while (readTotal < len) {
            val r = input.read(bytes, readTotal, len - readTotal)
            if (r == -1) break
            readTotal += r
        }
        return String(bytes, 0, readTotal, Charsets.UTF_8)
    }

    private fun readSentence(input: InputStream): List<String> {
        val words = mutableListOf<String>()
        while (true) {
            val word = readWord(input) ?: break
            if (word.isEmpty()) break
            words.add(word)
        }
        return words
    }

    private fun readResponseSentences(input: InputStream): List<List<String>> {
        val sentences = mutableListOf<List<String>>()
        while (true) {
            val sentence = readSentence(input)
            if (sentence.isEmpty()) break
            sentences.add(sentence)
            val first = sentence.firstOrNull()
            if (first == "!done" || first == "!trap") {
                break
            }
        }
        return sentences
    }

    private fun parseWordMap(sentence: List<String>): Map<String, String> {
        val map = mutableMapOf<String, String>()
        for (word in sentence) {
            if (word.startsWith("=")) {
                val idx = word.indexOf('=', 1)
                if (idx != -1) {
                    val key = word.substring(1, idx)
                    val value = word.substring(idx + 1)
                    map[key] = value
                }
            } else if (word.startsWith("?")) {
                val idx = word.indexOf('=', 1)
                if (idx != -1) {
                    val key = word.substring(1, idx)
                    val value = word.substring(idx + 1)
                    map[key] = value
                }
            }
        }
        return map
    }
}
