// -- gears_init.js

// Copyright 2007 Google Inc. All Rights Reserved.
//
// Sets up google.gears.*, which is *the only* supported way to access Gears.
//
// Circumvent this file at your own risk!
//
// In the future, Gears may automatically define google.gears.* without this
// file. Gears may use these objects to transparently fix bugs and compatibility
// issues. Applications that use the code below will continue to work seamlessly
// when that happens.

(function() {
  // -- protection for console logging (use Firebug / Lite)
  if (!("console" in window)) { 
    window.console = {
      'log': function(s) {
        //alert(s);
      }
    }
  }
  
  // We are already defined. Hooray!
  if (window.google && google.gears) {
    return;
  }

  var factory = null;

  // Firefox
  if (typeof GearsFactory != 'undefined') {
    factory = new GearsFactory();
  } else {
    // IE
    try {
      factory = new ActiveXObject('Gears.Factory');
    } catch (e) {
      // Safari
      if (navigator.mimeTypes["application/x-googlegears"]) {
        factory = document.createElement("object");
        factory.style.display = "none";
        factory.width = 0;
        factory.height = 0;
        factory.type = "application/x-googlegears";
        document.documentElement.appendChild(factory);
      }
    }
  }

  // *Do not* define any objects if Gears is not installed. This mimics the
  // behavior of Gears defining the objects in the future.
  if (!factory) {
    return;
  }

  // Now set up the objects, being careful not to overwrite anything.
  if (!window.google) {
    window.google = {};
  }

  if (!google.gears) {
    google.gears = {factory: factory};
  }
})();

// -- GearsDB itself!

function GearsDB(name, debug) {
  this.db = this.getDB(name);
  this.debug = (debug == 'true') ? true : false;
}

GearsDB.prototype.getDB = function(name) {
  if (this.debug) console.log("DB Name: " + name);
  try {
    var db = google.gears.factory.create('beta.database', '1.0');
    db.open(name);
    return db;
  } catch (e) {
    console.log('Could not get a handle to the database [' + name + ']: '+ e.message);
  }
}

// -- SELECT 

GearsDB.prototype.selectAll = function(sql, args, callback) {
  var rs = this.run(sql, args);
  if (!callback) {
    var total = [];
    callback = function(o) {
      total.push(o);
    }
    this.resultSetToObjects(rs, callback);
    return total;
  } else {
    return this.resultSetToObjects(rs, callback);
  }
}

GearsDB.prototype.selectOne = function(sql, args) {
  var rs = this.run(sql, args);
  return this.resultSetToObject(rs);  
}

GearsDB.prototype.selectRow = function(table, where, args, select) {
  return this.selectOne(this.selectSql(table, where, select), args);
}

GearsDB.prototype.selectRows = function(table, where, args, callback, select) {
  return this.selectAll(this.selectSql(table, where, select), args, callback);
}

GearsDB.prototype.run = function(sql, args) {
  try {
    var argvalue = '';
    if (args) argvalue = " with args: " + args.join(', ');
    if (this.debug) console.log("SQL: " + sql + argvalue);
    
    return this.db.execute(sql, args);
  } catch (e) {
    var argvalue = '';
    if (args) argvalue = " with args: " + args.join(', ');
    console.log("Trying to run: " + sql + argvalue + " producing error: " + e.message);
  }
}

GearsDB.prototype.insertRow = function(table, o, condition, conditionArgs) {
  if (condition) {
    var exists = this.selectOne('select rowid from ' + table + ' where ' + condition, conditionArgs);
    if (exists) {
      var argvalue = '';
      if (conditionArgs) argvalue = " with args: " + conditionArgs.join(', ');
      console.log("Row already exists for '" + condition + "' " + argvalue);
      return; // cut and run!
    }
  }
  var keys = [];
  var values = [];
  for (var x in o) {
    if (o.hasOwnProperty(x)) {
      keys.push(x);
      values.push(o[x]);
    }
  }

  this.run(this.insertSql(table, keys), values);
}

GearsDB.prototype.deleteRow = function(table, o) {
  var keys = [];
  var values = [];
  for (var x in o) {
    if (o.hasOwnProperty(x)) {
      keys.push(x);
      values.push(o[x]);
    }
  }

  this.run(this.deleteSql(table, keys), values);
}

GearsDB.prototype.updateRow = function(table, o, theId) {
  if (!theId) theId = 'id';
  var keys = [];
  var values = [];
  for (var x in o) {
    if (o.hasOwnProperty(x)) {
      keys.push(x);
      values.push(o[x]);
    }
  }
  values.push(o[theId]); // add on the id piece to the end

  this.run(this.updateSql(table, keys, theId), values);
}

GearsDB.prototype.forceRow = function(table, o, theId) {
  if (!theId) theId = 'id';
  
  var exists = this.selectRow(table, theId + ' = ?', [ o[theId] ], 'rowid');
  
  if (exists) {
    this.updateRow(table, o, theId);
  } else {
    this.insertRow(table, o);
  }  
}

GearsDB.prototype.dropTable = function(table) {
  this.run('delete from ' + table);
  this.run('drop table ' + table);
}

// -- Helpers

GearsDB.prototype.getColumns = function(rs) {
  var cols = rs.fieldCount();
  var colNames = [];
  for (var i = 0; i < cols; i++) {
    colNames.push(rs.fieldName(i));      
  }
  return colNames;
}

GearsDB.prototype.resultSetToObjects = function(rs, callback) {
  try {
    if (rs && rs.isValidRow()) {
      var columns = this.getColumns(rs);

      while (rs.isValidRow()) {
        var h = {};
        for (i = 0; i < columns.length; i++) {
          h[columns[i]] = rs.field(i);
        }
        callback(h);
        rs.next();
      }
    }
  } catch (e) {
    console.log(e.message);
  } finally {
    rs.close();
  }
}

GearsDB.prototype.resultSetToObject = function(rs) {
  try {
    if (rs && rs.isValidRow()) {
      var columns = this.getColumns(rs);

      var h = {};
      for (i = 0; i < columns.length; i++) {
        h[columns[i]] = rs.field(i);
      }
      return h;
    }
  } catch (e) {
    console.log(e);
  } finally {
    rs.close();
  }  
}

// -- SQL creators

GearsDB.prototype.selectSql = function(table, where, select) {
  if (!select) select = '*';
  return 'select ' + select + ' from ' + table + ' where ' + where;
}

GearsDB.prototype.insertSql = function(table, keys) {
  var placeholders = [];
  for (var i = 0; i < keys.length; i++) {
    placeholders.push('?');
  }
  return 'insert into ' + table + ' (' + keys.join(',') + ')' + " VALUES (" + placeholders.join(',') + ")";
}

GearsDB.prototype.deleteSql = function(table, keys) {
  var where = [];
  for (var i = 0; i < keys.length; i++) {
    where.push(keys[i] + '=?');
  }
  return 'delete from ' + table + ' where ' + where.join(' and ');
}

GearsDB.prototype.updateSql = function(table, keys, theId) {
  if (!theId) theId = 'id';
  var set = [];
  for (var i = 0; i < keys.length; i++) {
    set.push(keys[i] + '=?');
  }
  return 'update ' + table + ' set ' + set.join(', ') + ' where ' + theId + '= ?';
}

