/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * See the License for the specific language governing rights and
 * limitations under the License.
 *
 * The Original Code is Bespin.
 *
 * The Initial Developer of the Original Code is Mozilla.
 * Portions created by the Initial Developer are Copyright (C) 2009
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *   Bespin Team (bespin@mozilla.com)
 *
 * ***** END LICENSE BLOCK ***** */

if (typeof Bespin == "undefined") Bespin = {};
if (!Bespin.Settings) Bespin.Settings = {};

// = Settings =
//
// This settings module provides a base implementation to store settings for users.
// It also contains various "stores" to save that data, including:
//
// * {{{Bespin.Settings.Core}}} : Core interface to settings. User code always goes through here.
// * {{{Bespin.Settings.Server}}} : The main store. Saves back to the Bespin Server API
// * {{{Bespin.Settings.InMemory}}} : In memory settings that are used primarily for debugging
// * {{{Bespin.Settings.Cookie}}} : Store in a cookie using cookie-jar
// * {{{Bespin.Settings.URL}}} : Intercept settings in the URL. Often used to override
// * {{{Bespin.Settings.DB}}} : Commented out for now, but a way to store settings locally
// * {{{Bespin.Settings.Events}}} : Custom events that the settings store can intercept and send

// ** {{{ Bespin.Settings.Core }}} **
//
// Handles load/save of user settings.
// TODO: tie into the sessions servlet; eliminate Gears dependency

Bespin.Settings.Core = Class.create({
    initialize: function() {
        this.browserOverrides = {};
        this.fromURL = new Bespin.Settings.URL();
        this.customEvents = new Bespin.Settings.Events(this);

        this.loadStore();    // Load up the correct settings store
    },

    loadSession: function() {
        var path    = this.fromURL.get('path') || this.get('_path');
        var project = this.fromURL.get('project') || this.get('_project');

        document.fire("bespin:settings:init", { // -- time to init my friends
            path: path,
            project: project
        });
    },

    defaultSettings: function() {
        return {
            'tabsize': '2',
            'fontsize': '10',
            'autocomplete': 'off',
            'collaborate': 'off',
            'syntax': 'auto'
        };
    },

    // TODO: Make sure I can nuke the default function
    initSettings: function() {
        var self = this;

        document.fire("bespin:settings:set:collaborate", {
            value: self.get("collaborate")
        });
    },

    isOn: function(value) {
        return value == 'on' || value == 'true';
    },

    isOff: function(value) {
        return value == 'off' || value == 'false';
    },

    // ** {{{ Settings.loadStore() }}} **
    //
    // This is where we choose which store to load
    loadStore: function() {
        this.store = new Bespin.Settings.Server(this);

//        this.store = new Bespin.Settings.Cookie(this);

// TODO: ignore gears for now:
// this.store = (window['google']) ? new Bespin.Settings.DB : new Bespin.Settings.InMemory;
// this.store = new Bespin.Settings.InMemory;
    },

    toList: function() {
        var settings = [];
        var storeSettings = this.store.settings;
        for (var prop in storeSettings) {
            if (storeSettings.hasOwnProperty(prop)) {
                settings.push({ 'key': prop, 'value': storeSettings[prop] });
            }
        }
        return settings;
    },

    set: function(key, value) {
        this.store.set(key, value);

        document.fire("bespin:settings:set:" + key, { value: value });
    },

    get: function(key) {
        var fromURL = this.fromURL.get(key); // short circuit
        if (fromURL) return fromURL;

        return this.store.get(key);
    },

    unset: function(key) {
        this.store.unset(key);
    },

    list: function() {
        if (typeof this.store['list'] == "function") {
            return this.store.list();
        } else {
            return this.toList();
        }
    }

});

// ** {{{ Bespin.Settings.InMemory }}} **
//
// Debugging in memory settings (die when browser is closed)

Bespin.Settings.InMemory = Class.create({
    initialize: function(parent) {
        this.parent = parent;

        this.settings = this.parent.defaultSettings();

        document.fire("bespin:settings:loaded");
    },

    set: function(key, value) {
        this.settings[key] = value;
    },

    get: function(key) {
        return this.settings[key];
    },

    unset: function(key) {
        delete this.settings[key];
    }
});

// ** {{{ Bespin.Settings.Cookie }}} **
//
// Save the settings in a {{{CookieJar}}}

Bespin.Settings.Cookie = Class.create({
    initialize: function(parent) {
        this.parent = parent;
        this.jar = new CookieJar({
            expires: 3600, // seconds
            path: '/'
        });

        var fromJar = this.jar.get('settings');

        if (fromJar) {
            this.settings = fromJar;
        } else {
            this.settings = {
                'tabsize': '2',
                'fontsize': '10',
                'autocomplete': 'off',
                'collaborate': 'off',
                '_username': 'dion'
            };
            this.jar.put('settings', this.settings);
        }
        document.fire("bespin:settings:loaded");
    },

    set: function(key, value) {
        this.settings[key] = value;
        this.jar.put('settings', this.settings);
    },

    get: function(key) {
        return this.settings[key];
    },

    unset: function(key) {
        delete this.settings[key];
        this.jar.put('settings', this.settings);
    }
});

// ** {{{ Bespin.Settings.Server }}} **
//
// The real grand-daddy that implements uses {{{Server}}} to access the backend

Bespin.Settings.Server = Class.create({
    initialize: function(parent) {
        this.parent = parent;
        this.server = _server;

        // TODO: seed the settings
        this.server.listSettings(function(settings) {
            this.settings = settings;
            if (settings['tabsize'] == undefined) {
                this.settings = this.parent.defaultSettings();
                this.server.setSettings(this.settings);
            }

            document.fire("bespin:settings:loaded");
        }.bind(this));
    },

    set: function(key, value) {
        this.settings[key] = value;
        this.server.setSetting(key, value);
    },

    get: function(key) {
        return this.settings[key];
    },

    unset: function(key) {
        delete this.settings[key];
        this.unsetSetting(key);
    }
});


// ** {{{ Bespin.Settings.DB }}} **
//
// Taken out for now to allow us to not require gears_db.js (and Gears itself).
// Experimental ability to save locally in the SQLite database.
// The plan is to migrate to ActiveRecord.js or something like it to abstract on top
// of various stores (HTML5, Gears, globalStorage, etc.)

/*
// turn off for now so we can take gears_db.js out

Bespin.Settings.DB = Class.create({
    initialize: function(parent) {
        this.parent = parent;
        this.db = new GearsDB('wideboy');

        //this.db.run('drop table settings');
        this.db.run('create table if not exists settings (' +
               'id integer primary key,' +
               'key varchar(255) unique not null,' +
               'value varchar(255) not null,' +
               'timestamp int not null)');

        this.db.run('CREATE INDEX IF NOT EXISTS settings_id_index ON settings (id)');
        document.fire("bespin:settings:loaded");
    },

    set: function(key, value) {
        this.db.forceRow('settings', { 'key': key, 'value': value, timestamp: new Date().getTime() }, 'key');
    },

    get: function(key) {
        var rs = this.db.run('select distinct value from settings where key = ?', [ key ]);
        try {
            if (rs && rs.isValidRow()) {
              return rs.field(0);
            }
        } catch (e) {
            console.log(e.message);
        } finally {
            rs.close();
        }
    },

    unset: function(key) {
        this.db.run('delete from settings where key = ?', [ key ]);
    },

    list: function() {
        // TODO: Need to override with browser settings
        return this.db.selectRows('settings', '1=1');
    },

    // -- Private-y
    seed: function() {
        this.db.run('delete from settings');

        // TODO: loop through the settings
        this.db.run('insert into settings (key, value, timestamp) values (?, ?, ?)', ['keybindings', 'emacs', 1183878000000]);
        this.db.run('insert into settings (key, value, timestamp) values (?, ?, ?)', ['tabsize', '2', 1183878000000]);
        this.db.run('insert into settings (key, value, timestamp) values (?, ?, ?)', ['fontsize', '10', 1183878000000]);
        this.db.run('insert into settings (key, value, timestamp) values (?, ?, ?)', ['autocomplete', 'off', 1183878000000]);
    }
});
*/

// ** {{{ Bespin.Settings.URL }}} **
//
// Grab the setting from the URL, either via # or ?

Bespin.Settings.URL = Class.create({
    initialize: function(queryString) {
        this.queryString = this.stripHash(queryString || window.location.hash);
        this.results = this.queryString.toQueryParams();
    },

    get: function(key) {
        return this.results[key];
    },

    set: function(key, value) {
        this.results[key] = value;
    },
    
    stripHash: function(url) {
        var tobe = url.split('');
        tobe.shift();
        return tobe.join('');
    }
});

// ** {{{ Bespin.Settings.Events }}} **
//
// Custom Event holder for the Settings work. 
// It deals with both settings themselves, and other events that
// settings need to watch and look for

Bespin.Settings.Events = Class.create({
    initialize: function(settings) {
        this.settings = settings;

        // ** {{{ Event: bespin:settings:set }}} **
        // 
        // Watch for someone wanting to do a set operation
        document.observe("bespin:settings:set", function(event) {
            var key = event.memo.key;
            var value = event.memo.value;

            settings.set(key, value);
        });

        // ** {{{ Event: bespin:editor:openfile:opensuccess }}} **
        // 
        // Change the session settings when a new file is opened
        document.observe("bespin:editor:openfile:opensuccess", function(event) {
            var file = event.memo.file;

            _editSession.path = file.name;

            settings.set('_project',  _editSession.project);
            settings.set('_path',     _editSession.path);
            settings.set('_username', _editSession.username);

            if (_editSession.syncHelper) _editSession.syncHelper.syncWithServer();
        });

        // ** {{{ Event: bespin:editor:openfile:opensuccess }}} **
        // 
        // Change the syntax highlighter when a new file is opened
        document.observe("bespin:editor:openfile:opensuccess", function(event) {
            var file = event.memo.file;
            var type = file.name.split('.').last();

            if (type)
                document.fire("bespin:settings:syntax", { language: type });
        });

        // ** {{{ Event: bespin:settings:set:syntax }}} **
        // 
        // When the syntax setting is changed, tell the syntax system to change
        document.observe("bespin:settings:set:syntax", function(event) {
            var value = event.memo.value;
            
            document.fire("bespin:settings:syntax", { language: value, fromCommand: true });
        });

        // ** {{{ Event: bespin:settings:syntax }}} **
        // 
        // Given a new syntax command, change the editor.language        
        document.observe("bespin:settings:syntax", function(event) {
            var language = event.memo.language;
            var fromCommand = event.memo.fromCommand;
            var syntaxSetting = settings.get('syntax') || "off";

            if (language == _editor.language) return; // already set to be that language
            
            if (['auto', 'on'].include(language)) {
                var type = location.hash.split('.').last();
                if (type) _editor.language = type;
            } else if (['auto', 'on'].include(syntaxSetting) || fromCommand) {
                _editor.language = language;
            } else if (syntaxSetting == 'off') {
                _editor.language = 'off';
            }
        });

        // ** {{{ Event: bespin:settings:set:collaborate }}} **
        // 
        // Turn on the collaboration system if set to be on
        document.observe("bespin:settings:set:collaborate", function(event) {
            var value = event.memo.value;

            _editSession.collaborate = settings.isOn(value);
        });

        // ** {{{ Event: bespin:settings:set:fontsize }}} **
        // 
        // Change the font size for the editor
        document.observe("bespin:settings:set:fontsize", function(event) {
            var value = event.memo.value;

            var fontsize = parseInt(value);
            _editor.theme.lineNumberFont = fontsize + "pt Monaco, Lucida Console, monospace";
        });

        // ** {{{ Event: bespin:settings:set:theme }}} **
        // 
        // Change the Theme object used by the editor
        document.observe("bespin:settings:set:theme", function(event) {
            var theme = event.memo.value;

            if (theme) {
                var themeSettings = Bespin.Themes[theme];

                if (themeSettings) {
                    if (themeSettings != _editor.theme) {
                          _editor.theme = themeSettings;
                    }
                } else {
                    document.fire("bespin:cmdline:showinfo", {
                        msg: "Sorry old chap. No theme called '" + theme + "'. Fancy making it?"
                    });
                }
            }
        });

        // ** {{{ Event: bespin:settings:set:keybindings }}} **
        // 
        // Add in emacs key bindings
        document.observe("bespin:settings:set:keybindings", function(event) {
            var value = event.memo.value;

            if (value == "emacs") {
                document.fire("bespin:editor:bindkey", {
                    modifiers: "ctrl",
                    key: "b",
                    action: "moveCursorLeft"
                });

                document.fire("bespin:editor:bindkey", {
                    modifiers: "ctrl",
                    key: "f",
                    action: "moveCursorRight"
                });

                document.fire("bespin:editor:bindkey", {
                    modifiers: "ctrl",
                    key: "p",
                    action: "moveCursorUp"
                });

                document.fire("bespin:editor:bindkey", {
                    modifiers: "ctrl",
                    key: "n",
                    action: "moveCursorDown"
                });

                document.fire("bespin:editor:bindkey", {
                    modifiers: "ctrl",
                    key: "a",
                    action: "moveToLineStart"
                });

                document.fire("bespin:editor:bindkey", {
                    modifiers: "ctrl",
                    key: "e",
                    action: "moveToLineEnd"
                });
            }
        });
        
        // ** {{{ Event: bespin:settings:init }}} **
        // 
        // If we are opening up a new file
        document.observe("bespin:settings:init", function(event) {
            var path    = event.memo.path;
            var project = event.memo.project;

            // TODO: use the action and don't run a command itself
            var newfile = settings.fromURL.get('new');
            if (!newfile) { // scratch file
                if (project && (_editSession.project != project)) {
                    document.fire("bespin:project:set", { project: project });
                }

                if (path) {
                    document.fire("bespin:editor:openfile", { filename: path });
                }
            }
        });

        // ** {{{ Event: bespin:settings:init }}} **
        // 
        // Setup the theme
        document.observe("bespin:settings:init", function(event) {
            document.fire("bespin:settings:set:theme", {
                value: settings.get('theme')
            });
        });

        // ** {{{ Event: bespin:settings:init }}} **
        // 
        // Setup the special keybindings
        document.observe("bespin:settings:init", function(event) {
            document.fire("bespin:settings:set:keybindings", {
                value: settings.get('keybindings')
            });
        });

        // ** {{{ Event: bespin:settings:init }}} **
        // 
        // Check for auto load
        document.observe("bespin:settings:init", function(event) {
            if (settings.isOn(settings.get('autoconfig'))) {
                document.fire("bespin:editor:config:run");
            }
        });

        // ** {{{ Event: bespin:settings:init }}} **
        // 
        // Setup the font size that the user has configured
        document.observe("bespin:settings:init", function(event) {
            var fontsize = settings.get('fontsize');
            document.fire("bespin:settings:set:fontsize", {
                value: fontsize
            });
        });        
    }
});
