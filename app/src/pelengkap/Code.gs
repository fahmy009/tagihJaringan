function doGet(e) {
  // Jika diakses sebagai API oleh Android
  if (e && e.parameter && e.parameter.api === 'true') {
    var data = getAppData();
    return ContentService.createTextOutput(JSON.stringify(data))
      .setMimeType(ContentService.MimeType.JSON);
  }
  
  // Jika diakses dari Browser Web
  return HtmlService.createHtmlOutputFromFile('Index')
      .setTitle('Buku Tagihan WiFi')
      .setXFrameOptionsMode(HtmlService.XFrameOptionsMode.ALLOWALL)
      .addMetaTag('viewport', 'width=device-width, initial-scale=1');
}

function onOpen() {
  var ui = SpreadsheetApp.getUi();
  ui.createMenu('⚙️ Buku Tagihan')
      .addItem('Setup Database & Data Dummy', 'setupDatabase')
      .addItem('Setup Database User & Setoran', 'setupUserDatabase')
      .addToUi();
}

function setupDatabase() {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  
  // 1. SETUP SHEET "Data Pelanggan"
  var sheetPelanggan = ss.getSheetByName("Data Pelanggan");
  if (!sheetPelanggan) {
    if (ss.getSheets().length === 1 && ss.getSheets()[0].getLastRow() === 0) {
      sheetPelanggan = ss.getSheets()[0];
      sheetPelanggan.setName("Data Pelanggan");
    } else {
      sheetPelanggan = ss.insertSheet("Data Pelanggan");
    }
  }
  
  if (sheetPelanggan.getLastRow() === 0) {
    sheetPelanggan.appendRow(["NAMA", "COMMENT", "MODEM", "PROFILE", "NOMINAL", "BAYAR", "PENAGIH"]);
    sheetPelanggan.getRange("A1:G1").setFontWeight("bold").setBackground("#E2E8F0");
    
    var dummyData = [
      ["adel", "SERVER - TOTOLINK", "TOTOLINK", "SUMBERGADUNG", 95000, "", ""],
      ["ameliaSGP", "ODP AMELIA - PETTUNG - TENDA", "TENDA", "SUMBERGADUNG", 95000, "", ""],
      ["ases", "ODP HAR - SG - TENDA", "TENDA", "SUMBERGADUNG", 95000, "", ""],
      ["buklastri", "ODP PRI - SG - TENDA", "TENDA", "SUMBERGADUNG", 95000, "", ""],
      ["bukrobiSGP", "ODP BUK ROBI - PETTUNG - TOTOLINK", "TOTOLINK", "SUMBERGADUNG", 95000, "", ""],
      ["dani", "ODP PARTELON - SG - TOTOLINK", "TOTOLINK", "SUMBERGADUNG", 95000, "", ""],
      ["desi", "ODP NIWAR - SG - TENDA", "SG - TENDA", "SUMBERGADUNG", 95000, "", ""]
    ];
    
    sheetPelanggan.getRange(2, 1, dummyData.length, 7).setValues(dummyData);
    sheetPelanggan.autoResizeColumns(1, 7);
  }
  
  // 2. SETUP SHEET "Pengaturan"
  var sheetPengaturan = ss.getSheetByName("Pengaturan");
  if (!sheetPengaturan) {
    sheetPengaturan = ss.insertSheet("Pengaturan");
  }
  
  if (sheetPengaturan.getLastRow() === 0) {
    sheetPengaturan.appendRow(["ID Potongan", "Nama Label", "Nominal Default"]);
    sheetPengaturan.getRange("A1:C1").setFontWeight("bold").setBackground("#E2E8F0");
    
    var defaultSettings = [
      ["potA", "Potongan A", 0],
      ["potB", "Potongan B", 0],
      ["potC", "Potongan C", 0],
      ["potD", "Potongan D", 0]
    ];
    
    sheetPengaturan.getRange(2, 1, defaultSettings.length, 3).setValues(defaultSettings);
    sheetPengaturan.autoResizeColumns(1, 3);
  }

  setupUserDatabase();

  SpreadsheetApp.getUi().alert("✅ Database Berhasil Disiapkan!\n\nHeader telah disesuaikan dengan format WiFi Anda.");
}

function setupUserDatabase() {
  var ss = SpreadsheetApp.getActiveSpreadsheet();

  // 1. Sheet User_Account
  var sheetUser = ss.getSheetByName("User_Account");
  if (!sheetUser) {
    sheetUser = ss.insertSheet("User_Account");
  }
  if (sheetUser.getLastRow() === 0) {
    sheetUser.appendRow(["USERNAME", "PASSWORD", "ROLE", "ASSIGNED_PROFILE", "NAME"]);
    sheetUser.getRange("A1:E1").setFontWeight("bold").setBackground("#E2E8F0");
    sheetUser.appendRow(["admin", "admin", "ADMIN", "", "Administrator"]);
    sheetUser.appendRow(["mitra1", "1234", "MITRA", "SUMBERGADUNG", "Mitra Sumbergadung"]);
    sheetUser.appendRow(["tagih1", "1234", "COLLECTOR", "SUMBERGADUNG", "Tukang Tagih A"]);
    sheetUser.autoResizeColumns(1, 5);
  }

  // 2. Sheet Riwayat_Setoran
  var sheetSetoran = ss.getSheetByName("Riwayat_Setoran");
  if (!sheetSetoran) {
    sheetSetoran = ss.insertSheet("Riwayat_Setoran");
  }
  if (sheetSetoran.getLastRow() === 0) {
    sheetSetoran.appendRow(["ID_SETOR", "TANGGAL_JAM", "COLLECTOR_NAME", "MITRA_NAME", "PROFILE", "NOMINAL_SETOR", "SISA_PEGANGAN", "STATUS", "DITERIMA_MITRA", "HUTANG_PENAGIH"]);
    sheetSetoran.getRange("A1:J1").setFontWeight("bold").setBackground("#E2E8F0");
    sheetSetoran.autoResizeColumns(1, 10);
  }
}

function getAppData() {
  return {
    customers: getCustomersData(),
    settings: getSettings(),
    setoran: getSetoranData(),
    users: getUsersData(),
    mikrotik: getMikrotikConfig()
  };
}

function getUsersData() {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var sheet = ss.getSheetByName("User_Account");
  if (!sheet) {
    setupUserDatabase();
    sheet = ss.getSheetByName("User_Account");
  }

  var data = sheet.getDataRange().getValues();
  var list = [];

  for (var i = 1; i < data.length; i++) {
    if (data[i][0]) {
      list.push({
        username: data[i][0] ? data[i][0].toString() : "",
        role: data[i][2] ? data[i][2].toString().toUpperCase() : "ADMIN",
        assignedProfile: data[i][3] ? data[i][3].toString() : "",
        name: data[i][4] ? data[i][4].toString() : (data[i][0] ? data[i][0].toString() : ""),
        rowIndex: i
      });
    }
  }
  return list;
}

function getSetoranData() {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var sheet = ss.getSheetByName("Riwayat_Setoran");
  if (!sheet) {
    setupUserDatabase();
    sheet = ss.getSheetByName("Riwayat_Setoran");
  }

  var data = sheet.getDataRange().getValues();
  var list = [];

  for (var i = 1; i < data.length; i++) {
    if (data[i][0] || data[i][1]) {
      var amt = Number(data[i][5]) || 0;
      var rec = (data[i][8] !== undefined && data[i][8] !== "") ? Number(data[i][8]) : amt;
      var hutang = (data[i][9] !== undefined && data[i][9] !== "") ? Number(data[i][9]) : 0;
      list.push({
        id: data[i][0] ? data[i][0].toString() : "setor_" + i,
        date: data[i][1] ? data[i][1].toString() : "",
        collectorName: data[i][2] ? data[i][2].toString() : "",
        mitraName: data[i][3] ? data[i][3].toString() : "",
        profile: data[i][4] ? data[i][4].toString() : "",
        amount: amt,
        sisaPegangan: Number(data[i][6]) || 0,
        status: data[i][7] ? data[i][7].toString() : "PENDING",
        amountReceived: rec,
        selisihHutang: hutang,
        rowIndex: i
      });
    }
  }
  return list;
}

function authenticateUser(username, password) {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var sheet = ss.getSheetByName("User_Account");
  if (!sheet) {
    setupUserDatabase();
    sheet = ss.getSheetByName("User_Account");
  }

  var data = sheet.getDataRange().getValues();
  var cleanUser = (username || "").toString().trim().toLowerCase();
  var cleanPass = (password || "").toString().trim();

  for (var i = 1; i < data.length; i++) {
    var u = data[i][0] ? data[i][0].toString().trim().toLowerCase() : "";
    var p = data[i][1] ? data[i][1].toString().trim() : "";
    if (u === cleanUser && p === cleanPass) {
      return {
        username: data[i][0].toString(),
        role: data[i][2] ? data[i][2].toString().toUpperCase() : "ADMIN",
        assignedProfile: data[i][3] ? data[i][3].toString() : "",
        name: data[i][4] ? data[i][4].toString() : data[i][0].toString()
      };
    }
  }
  return null;
}

function updateUserProfile(username, newPassword, newName) {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var sheet = ss.getSheetByName("User_Account");
  if (!sheet) return "Sheet User_Account tidak ditemukan!";

  var data = sheet.getDataRange().getValues();
  var cleanUser = (username || "").toString().trim().toLowerCase();

  for (var i = 1; i < data.length; i++) {
    var u = data[i][0] ? data[i][0].toString().trim().toLowerCase() : "";
    if (u === cleanUser) {
      if (newPassword && newPassword.toString().trim() !== "") {
        sheet.getRange(i + 1, 2).setValue(newPassword.toString().trim());
      }
      if (newName && newName.toString().trim() !== "") {
        sheet.getRange(i + 1, 5).setValue(newName.toString().trim());
      }
      return "Profil dan Password berhasil diperbarui!";
    }
  }
  return "User tidak ditemukan!";
}

function saveUserAccount(params) {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var sheet = ss.getSheetByName("User_Account");
  if (!sheet) {
    setupUserDatabase();
    sheet = ss.getSheetByName("User_Account");
  }

  var data = sheet.getDataRange().getValues();
  var cleanUser = (params.username || "").toString().trim().toLowerCase();
  var cleanPass = (params.password || "").toString().trim();
  var role = (params.role || "COLLECTOR").toString().trim().toUpperCase();
  var profile = (params.assignedProfile || "").toString().trim();
  var name = (params.name || params.username || "").toString().trim();

  if (!cleanUser) {
    return { status: "error", message: "Username tidak boleh kosong!" };
  }

  var targetRow = -1;
  for (var i = 1; i < data.length; i++) {
    var u = data[i][0] ? data[i][0].toString().trim().toLowerCase() : "";
    if (u === cleanUser) {
      targetRow = i + 1;
      break;
    }
  }

  if (targetRow > 0) {
    // Edit existing user
    if (cleanPass) sheet.getRange(targetRow, 2).setValue(cleanPass);
    sheet.getRange(targetRow, 3).setValue(role);
    sheet.getRange(targetRow, 4).setValue(profile);
    if (name) sheet.getRange(targetRow, 5).setValue(name);
    return { status: "success", message: "Akun '" + cleanUser + "' berhasil diperbarui!" };
  } else {
    // New user creation
    if (!cleanPass) {
      return { status: "error", message: "Password wajib diisi untuk akun baru!" };
    }
    sheet.appendRow([params.username.toString().trim(), cleanPass, role, profile, name]);
    return { status: "success", message: "Akun baru '" + cleanUser + "' berhasil dibuat!" };
  }
}

function deleteUserAccount(username) {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var sheet = ss.getSheetByName("User_Account");
  if (!sheet) return "Sheet User_Account tidak ditemukan!";

  var data = sheet.getDataRange().getValues();
  var cleanUser = (username || "").toString().trim().toLowerCase();

  for (var i = data.length - 1; i >= 1; i--) {
    var u = data[i][0] ? data[i][0].toString().trim().toLowerCase() : "";
    if (u === cleanUser) {
      if (u === "admin") return "Akun admin utama tidak boleh dihapus!";
      sheet.deleteRow(i + 1);
      return "Akun '" + cleanUser + "' berhasil dihapus!";
    }
  }
  return "Username tidak ditemukan!";
}

function submitSetoranData(params) {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var sheet = ss.getSheetByName("Riwayat_Setoran");
  if (!sheet) {
    setupUserDatabase();
    sheet = ss.getSheetByName("Riwayat_Setoran");
  }

  var id = "setor_" + (sheet.getLastRow() + 1) + "_" + new Date().getTime();
  var dateStr = Utilities.formatDate(new Date(), Session.getScriptTimeZone(), "dd-MM-yyyy HH:mm:ss");

  var cName = params.collectorName || "";
  var mName = params.mitraName || "";
  var profile = params.profile || "";
  var amount = Number(params.amount) || 0;
  var sisa = Number(params.sisaPegangan) || 0;
  var status = "PENDING";

  sheet.appendRow([id, dateStr, cName, mName, profile, amount, sisa, status, amount, 0]);
  return "Setoran sebesar Rp" + amount.toLocaleString('id-ID') + " berhasil diajukan!";
}

function verifySetoranData(params) {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var sheet = ss.getSheetByName("Riwayat_Setoran");
  if (!sheet) return "Sheet Riwayat_Setoran tidak ditemukan!";

  var setoranId = (typeof params === 'object') ? params.setoranId : params;
  var rec = (params && params.amountReceived !== undefined && params.amountReceived !== null) ? Number(params.amountReceived) : null;
  var hutang = (params && params.selisihHutang !== undefined && params.selisihHutang !== null) ? Number(params.selisihHutang) : 0;

  var data = sheet.getDataRange().getValues();
  for (var i = 1; i < data.length; i++) {
    if (data[i][0] && data[i][0].toString() === setoranId.toString()) {
      var origAmount = Number(data[i][5]) || 0;
      if (rec === null) rec = origAmount;
      sheet.getRange(i + 1, 8).setValue("VERIFIED");
      sheet.getRange(i + 1, 9).setValue(rec);
      sheet.getRange(i + 1, 10).setValue(hutang);
      return "Setoran berhasil diverifikasi! (Diterima: Rp" + rec.toLocaleString('id-ID') + ")";
    }
  }
  return "ID Setoran tidak ditemukan!";
}

function getMikrotikConfig() {
  var props = PropertiesService.getScriptProperties();
  return {
    ip: props.getProperty('MIK_IP') || "",
    user: props.getProperty('MIK_USER') || "",
    pass: props.getProperty('MIK_PASS') || "",
    profile: props.getProperty('MIK_PROFILE') || "",
    active: props.getProperty('MIK_API_ACTIVE') === 'true'
  };
}

function saveMikrotikConfig(config) {
  var props = PropertiesService.getScriptProperties();
  props.setProperty('MIK_IP', config.ip);
  props.setProperty('MIK_USER', config.user);
  props.setProperty('MIK_PASS', config.pass);
  props.setProperty('MIK_PROFILE', config.profile);
  props.setProperty('MIK_API_ACTIVE', config.active ? 'true' : 'false');
  return "Pengaturan MikroTik Berhasil Disimpan!";
}

function getCustomersData() {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var sheet = ss.getSheetByName("Data Pelanggan") || ss.getSheets()[0]; 
  
  var data = sheet.getDataRange().getValues();
  var customers = [];
  
  for (var i = 1; i < data.length; i++) {
    var name = data[i][0];       
    var comment = data[i][1];    
    var modem = data[i][2];      
    var profile = data[i][3];    
    var rawNominal = data[i][4]; 
    var bayar = data[i][5];      
    var collectedBy = data[i][6] || "";

    var nominal = 0;
    if (typeof rawNominal === 'number') {
      nominal = rawNominal;
    } else if (typeof rawNominal === 'string') {
      nominal = Number(rawNominal.replace(/[^0-9.-]+/g,"")) || 0;
    }
    
    if (name) {
      customers.push({
        id: i,
        rowIndex: i,
        name: name,
        comment: comment || "",
        modem: modem || "",
        profile: profile || "",
        nominal: nominal,
        bayar: (bayar === "") ? "" : bayar,
        collectedBy: collectedBy ? collectedBy.toString() : ""
      });
    }
  }
  return customers;
}

function getSettings() {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var sheet = ss.getSheetByName("Pengaturan");
  
  if (!sheet) {
    setupDatabase();
    sheet = ss.getSheetByName("Pengaturan");
  }
  
  var data = sheet.getDataRange().getValues();
  var potongan = [];
  
  for (var i = 1; i < data.length; i++) {
    if (data[i][0] || data[i][1]) {
      var pId = data[i][0] ? data[i][0].toString() : "pot_" + i;
      var pLabel = data[i][1] ? data[i][1].toString() : "";
      var rawVal = data[i][2];
      var pProfile = data[i][3] ? data[i][3].toString() : "";
      var pAmount = 0;
      if (typeof rawVal === 'number') {
        pAmount = rawVal;
      } else if (typeof rawVal === 'string') {
        pAmount = Number(rawVal.replace(/[^0-9.-]+/g,"")) || 0;
      }
      potongan.push({
        id: pId,
        name: pLabel,
        amount: pAmount,
        profile: pProfile
      });
    }
  }
  return { potongan: potongan };
}

function saveSettings(settings) {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var sheet = ss.getSheetByName("Pengaturan");
  
  if (!sheet) {
    setupDatabase(); 
    sheet = ss.getSheetByName("Pengaturan");
  }
  
  var lastRow = sheet.getLastRow();
  if (lastRow > 1) {
    sheet.getRange(2, 1, lastRow - 1, 4).clearContent();
  }
  
  var rows = [];
  if (settings && settings.potongan && Array.isArray(settings.potongan)) {
    for (var i = 0; i < settings.potongan.length; i++) {
      var p = settings.potongan[i];
      if (p) {
        var pId = p.id || ("pot_" + (i + 1));
        var pName = p.name || "";
        var pAmount = p.amount || 0;
        var pProfile = p.profile || "";
        rows.push([pId, pName, pAmount, pProfile]);
      }
    }
  } else if (settings && typeof settings === 'object') {
    var keys = Object.keys(settings).filter(function(k) { return !k.endsWith('_val'); });
    for (var j = 0; j < keys.length; j++) {
      var id = keys[j];
      var label = settings[id];
      var val = settings[id + "_val"] || 0;
      rows.push([id, label, val, ""]);
    }
  }
  
  if (rows.length > 0) {
    sheet.getRange(2, 1, rows.length, 4).setValues(rows);
  }
  
  return true;
}

function updatePaymentStatus(rowIndex, isChecked, nominal, collectedBy) {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var sheet = ss.getSheetByName("Data Pelanggan");
  var row = rowIndex + 1; 
  
  if (isChecked) {
    sheet.getRange(row, 6).setValue(nominal);
    if (collectedBy) {
      sheet.getRange(row, 7).setValue(collectedBy);
    }
  } else {
    sheet.getRange(row, 6).setValue("");
    sheet.getRange(row, 7).setValue("");
  }
  
  var mik = getMikrotikConfig();
  if (mik.active) {
    var namaPelanggan = sheet.getRange(row, 1).getValue();
    setMikrotikSecretState(namaPelanggan, !isChecked, mik);
  }
}

function updateNominalPelanggan(rowIndex, newNominal) {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var sheet = ss.getSheetByName("Data Pelanggan");
  if (!sheet) return;
  var row = rowIndex + 1; 
  sheet.getRange(row, 5).setValue(newNominal);
}

function tutupBukuBulanIni() {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var sheet = ss.getSheetByName("Data Pelanggan");
  if (!sheet) return "Sheet Data Pelanggan tidak ditemukan!";
  
  var range = sheet.getDataRange();
  var data = range.getValues();
  
  var gross = 0;
  var countLunas = 0;
  
  // Hitung pemasukan sebelum direset
  for (var i = 1; i < data.length; i++) {
    var rawNominal = data[i][4];
    var bayar = data[i][5];
    
    var nominal = 0;
    if (typeof rawNominal === 'number') {
      nominal = rawNominal;
    } else if (typeof rawNominal === 'string') {
      nominal = Number(rawNominal.replace(/[^0-9.-]+/g,"")) || 0;
    }
    
    // Jika sudah bayar
    if (bayar !== "" && bayar !== 0) {
      gross += nominal;
      countLunas++;
    }
    
    // Reset status pembayaran hanya untuk yang nominalnya BUKAN 0 (Bukan gratis)
    if (nominal !== 0) {
      data[i][5] = ""; // Reset kolom F (Index ke 5)
      data[i][6] = ""; // Reset kolom G (Index ke 6: PENAGIH)
    }
  }
  
  // Tulis ulang seluruh data sheet sekaligus
  range.setValues(data);
  
  // Hitung Total Potongan dari Sheet Pengaturan
  var sheetPengaturan = ss.getSheetByName("Pengaturan");
  var totalPotongan = 0;
  if (sheetPengaturan) {
    var pData = sheetPengaturan.getDataRange().getValues();
    for (var p = 1; p < pData.length; p++) {
      var pVal = pData[p][2];
      if (typeof pVal === 'number') {
        totalPotongan += pVal;
      } else if (typeof pVal === 'string') {
        totalPotongan += Number(pVal.replace(/[^0-9.-]+/g,"")) || 0;
      }
    }
  }
  
  var net = gross - totalPotongan;
  
  // Catat Laporan ke Sheet "Laporan Bulanan"
  var laporanSheet = ss.getSheetByName("Laporan Bulanan");
  if (!laporanSheet) {
    laporanSheet = ss.insertSheet("Laporan Bulanan");
    laporanSheet.appendRow(["Tanggal Tutup Buku", "Total Pelanggan Lunas", "Pemasukan Kotor", "Total Potongan", "Hasil Bersih"]);
    laporanSheet.getRange("A1:E1").setFontWeight("bold").setBackground("#E2E8F0");
  }
  
  var today = Utilities.formatDate(new Date(), Session.getScriptTimeZone(), "dd-MM-yyyy HH:mm:ss");
  laporanSheet.appendRow([today, countLunas, gross, totalPotongan, net]);
  laporanSheet.autoResizeColumns(1, 5);
  
  return "Tutup Buku Berhasil!\nPendapatan bersih sebesar " + net.toLocaleString('id-ID') + " telah dicatat ke sheet Laporan Bulanan, dan seluruh tagihan telah di-reset untuk bulan depan.";
}

// ================= API UNTUK APLIKASI ANDROID NATIVE =================
function doPost(e) {
  // Tangkap request dari Android
  if (!e || !e.postData || !e.postData.contents) {
    return ContentService.createTextOutput(JSON.stringify({ status: "error", message: "Tidak ada data payload" })).setMimeType(ContentService.MimeType.JSON);
  }
  
  var params;
  try {
    params = JSON.parse(e.postData.contents);
  } catch(err) {
    return ContentService.createTextOutput(JSON.stringify({ status: "error", message: "JSON tidak valid" })).setMimeType(ContentService.MimeType.JSON);
  }
  
  var action = params.action;
  
  try {
    if (action === "login") {
      var user = authenticateUser(params.username, params.password);
      if (user) {
        return ContentService.createTextOutput(JSON.stringify({ status: "success", user: user })).setMimeType(ContentService.MimeType.JSON);
      } else {
        return ContentService.createTextOutput(JSON.stringify({ status: "error", message: "Username atau Password salah!" })).setMimeType(ContentService.MimeType.JSON);
      }
    }
    else if (action === "updateUserProfile") {
      var msgProfile = updateUserProfile(params.username, params.newPassword, params.newName);
      return ContentService.createTextOutput(JSON.stringify({ status: "success", message: msgProfile })).setMimeType(ContentService.MimeType.JSON);
    }
    else if (action === "saveUserAccount") {
      var resUser = saveUserAccount(params);
      return ContentService.createTextOutput(JSON.stringify(resUser)).setMimeType(ContentService.MimeType.JSON);
    }
    else if (action === "deleteUserAccount") {
      var msgDelUser = deleteUserAccount(params.username);
      return ContentService.createTextOutput(JSON.stringify({ status: "success", message: msgDelUser })).setMimeType(ContentService.MimeType.JSON);
    }
    else if (action === "submitSetoran") {
      var msgSetor = submitSetoranData(params);
      return ContentService.createTextOutput(JSON.stringify({ status: "success", message: msgSetor })).setMimeType(ContentService.MimeType.JSON);
    }
    else if (action === "verifySetoran") {
      var msgVerify = verifySetoranData(params);
      return ContentService.createTextOutput(JSON.stringify({ status: "success", message: msgVerify })).setMimeType(ContentService.MimeType.JSON);
    }
    else if (action === "updatePayment") {
      updatePaymentStatus(params.rowIndex, params.isChecked, params.nominal, params.collectedBy);
      return ContentService.createTextOutput(JSON.stringify({ status: "success" })).setMimeType(ContentService.MimeType.JSON);
    }
    else if (action === "saveSettings") {
      saveSettings(params.settings);
      return ContentService.createTextOutput(JSON.stringify({ status: "success" })).setMimeType(ContentService.MimeType.JSON);
    }
    else if (action === "tutupBuku") {
      var msg = tutupBukuBulanIni();
      return ContentService.createTextOutput(JSON.stringify({ status: "success", message: msg })).setMimeType(ContentService.MimeType.JSON);
    }
    else if (action === "updateNominal") {
      updateNominalPelanggan(params.rowIndex, params.nominal);
      return ContentService.createTextOutput(JSON.stringify({ status: "success" })).setMimeType(ContentService.MimeType.JSON);
    }
    else if (action === "syncMikrotik") {
      var msg = syncMikrotikManual();
      return ContentService.createTextOutput(JSON.stringify({ status: "success", message: msg })).setMimeType(ContentService.MimeType.JSON);
    }
    else if (action === "saveMikrotik") {
      var msg2 = saveMikrotikConfig(params.config);
      return ContentService.createTextOutput(JSON.stringify({ status: "success", message: msg2 })).setMimeType(ContentService.MimeType.JSON);
    }
    else if (action === "syncNewCustomers") {
      var ss = SpreadsheetApp.getActiveSpreadsheet();
      var sheet = ss.getSheetByName("Data Pelanggan") || ss.getSheets()[0];
      var newCusts = params.newCustomers || [];
      var countAdded = 0;

      for (var k = 0; k < newCusts.length; k++) {
        var c = newCusts[k];
        if (c && c.name) {
          var nom = (c.nominal !== undefined && c.nominal !== null) ? Number(c.nominal) : 0;
          if (isNaN(nom)) nom = 0;

          var cComment = (c.comment || "").toString();
          if (nom === 0 && cComment.toLowerCase().indexOf("server") === -1) {
            nom = 95000;
          }

          var cModem = c.modem || "";
          if (!cModem && cComment.trim()) {
            var parts = cComment.trim().split(/[\s-]+/);
            if (parts.length > 0) cModem = parts[parts.length - 1];
          }

          // Baris: NAMA, COMMENT, MODEM, PROFILE, NOMINAL, BAYAR, PENAGIH(kosong)
          sheet.appendRow([c.name, cComment, cModem, c.profile || "", nom, "", ""]);
          countAdded++;
        }
      }
      return ContentService.createTextOutput(JSON.stringify({ status: "success", message: countAdded + " pelanggan baru ditambahkan" })).setMimeType(ContentService.MimeType.JSON);
    }
    else if (action === "deleteCustomers") {
      var ss = SpreadsheetApp.getActiveSpreadsheet();
      var sheet = ss.getSheetByName("Data Pelanggan") || ss.getSheets()[0];
      var namesToDelete = params.names || [];
      if (!Array.isArray(namesToDelete)) namesToDelete = [];

      var data = sheet.getDataRange().getValues();
      var countDeleted = 0;

      var nameMap = {};
      for (var d = 0; d < namesToDelete.length; d++) {
        if (namesToDelete[d]) nameMap[namesToDelete[d].toString().trim().toLowerCase()] = true;
      }

      for (var r = data.length - 1; r >= 1; r--) {
        var rowName = data[r][0] ? data[r][0].toString().trim().toLowerCase() : "";
        if (rowName && nameMap[rowName]) {
          sheet.deleteRow(r + 1);
          countDeleted++;
        }
      }
      return ContentService.createTextOutput(JSON.stringify({ status: "success", message: countDeleted + " pelanggan dihapus" })).setMimeType(ContentService.MimeType.JSON);
    }
  } catch(error) {
    return ContentService.createTextOutput(JSON.stringify({ status: "error", message: error.toString() })).setMimeType(ContentService.MimeType.JSON);
  }
  
  return ContentService.createTextOutput(JSON.stringify({ status: "error", message: "Aksi tidak dikenali" })).setMimeType(ContentService.MimeType.JSON);
}

// ================= FUNGSI MIKROTIK =================
function setMikrotikSecretState(namaPelanggan, isDisabled, mik) {
  if(!mik) mik = getMikrotikConfig();
  var urlBase = "https://" + mik.ip + "/rest/ppp";
  var headers = {
    "Authorization": "Basic " + Utilities.base64Encode(mik.user + ":" + mik.pass),
    "Content-Type": "application/json"
  };
  var options = {
    "headers": headers,
    "muteHttpExceptions": true,
    "validateHttpsCertificates": false
  };
  
  // 1. Matikan atau Hidupkan Secret
  var urlSecret = urlBase + "/secret/" + encodeURIComponent(namaPelanggan);
  options.method = "patch";
  options.payload = JSON.stringify({"disabled": isDisabled ? "true" : "false"});
  UrlFetchApp.fetch(urlSecret, options);
  
  // 2. Jika dimatikan (isolir), kick sesi aktifnya!
  if (isDisabled) {
    var urlGetActive = urlBase + "/active?name=" + encodeURIComponent(namaPelanggan);
    options.method = "get";
    delete options.payload; // jangan kirim payload saat GET
    
    var res = UrlFetchApp.fetch(urlGetActive, options);
    if (res.getResponseCode() === 200) {
      try {
        var activeList = JSON.parse(res.getContentText());
        if (activeList && activeList.length > 0) {
          var idAktif = activeList[0][".id"];
          var urlKick = urlBase + "/active/" + idAktif;
          options.method = "delete";
          UrlFetchApp.fetch(urlKick, options);
        }
      } catch(e) {}
    }
  }
}

function syncMikrotikManual() {
  var mik = getMikrotikConfig();
  if (!mik.active) return "Sistem API MikroTik belum diaktifkan (Nyalakan dari menu Pengaturan).";
  
  var urlBase = "https://" + mik.ip + "/rest/ppp/secret";
  if (mik.profile && mik.profile.trim() !== "") {
    urlBase += "?profile=" + encodeURIComponent(mik.profile.trim());
  }
  
  var headers = {
    "Authorization": "Basic " + Utilities.base64Encode(mik.user + ":" + mik.pass),
    "Content-Type": "application/json"
  };
  var options = {
    "method": "get",
    "headers": headers,
    "muteHttpExceptions": true,
    "validateHttpsCertificates": false
  };
  
  try {
    var res = UrlFetchApp.fetch(urlBase, options);
    if (res.getResponseCode() !== 200) {
      return "Gagal terhubung ke MikroTik. (Kode: " + res.getResponseCode() + ")";
    }
    
    var secrets = JSON.parse(res.getContentText());
    var ss = SpreadsheetApp.getActiveSpreadsheet();
    var sheet = ss.getSheetByName("Data Pelanggan");
    if (!sheet) return "Sheet Data Pelanggan tidak ditemukan!";
    
    var data = sheet.getDataRange().getValues();
    var existingNames = {};
    for (var i = 1; i < data.length; i++) {
      var nm = data[i][0];
      if (nm) existingNames[nm.toString().toLowerCase()] = true;
    }
    
    var ditambahkan = 0;
    for (var j = 0; j < secrets.length; j++) {
      var sec = secrets[j];
      var sName = sec.name;
      if (sName && !existingNames[sName.toLowerCase()]) {
        var sComment = sec.comment || "";
        var sProfile = sec.profile || "";
        var isSrv = sComment.toLowerCase().indexOf("server") !== -1;
        var defaultNom = isSrv ? 0 : 95000;
        var modemWord = "";
        if (sComment.trim()) {
          var parts = sComment.trim().split(/[\s-]+/);
          if (parts.length > 0) modemWord = parts[parts.length - 1];
        }
        // Baris: NAMA, COMMENT, MODEM, PROFILE, NOMINAL, BAYAR, PENAGIH(kosong)
        sheet.appendRow([sName, sComment, modemWord, sProfile, defaultNom, "", ""]);
        ditambahkan++;
      }
    }
    return "Sinkronisasi Berhasil! " + ditambahkan + " pelanggan baru ditambahkan dari MikroTik.";
  } catch(error) {
    return "Terjadi kesalahan: " + error.toString();
  }
}
