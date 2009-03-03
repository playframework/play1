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
 
// = Bootstrap =
//
// This file is the editor bootstrap code that is loaded via script src
// from /editor.html.
// 
// It handles setting up the objects that are to be used on the editor
// and deals with layout changes.

// ** {{{ Globals }}}
//
// One day we will get rid of all of these bar the core Bespin / _ object.
var _ = Bespin; // alias away!

var _editor;
var _editSession;
var _commandLine;
var _files;
var _settings;
var _server;
var _toolbar;
var _projectLabel;
var _fileLabel;
var _scene;

var _showCollab = _showFiles = _showTarget = false; // for layout
var _showCollabHotCounter = 0;

// ** {{{ window.load time }}} **
//
// Loads and configures the objects that the editor needs
Event.observe(document, "dom:loaded", function() {
    _editor      = new Bespin.Editor.API($('editor'));
    _editSession = new Bespin.Session.EditSession(_editor);
    _server      = new Bespin.Server('/bespin');
    _files       = new Bespin.FileSystem();
    _toolbar     = new Bespin.Editor.Toolbar();

    _toolbar.setupDefault();

    _editor.setFocus(true);
    
    // Force a login just in case the user session isn't around
    _server.currentuser(isLoggedIn, isNotLoggedIn);
    
    // Set the version info
    Bespin.displayVersion();

    // Get going when settings are loaded
    document.observe("bespin:settings:loaded", function(event) {
        _settings.loadSession();  // load the last file or what is passed in
        doResize();
    });

    Element.observe(window, 'resize', doResize);

    _scene = new Scene($("projectLabel"));

    var panel = new Panel();
    _scene.root.add(panel);

    _projectLabel = new Label({ style: {
        color: "white",
        font: "12pt Calibri, Arial, sans-serif"
    }});
    var symbolThingie = new Label({ text: ":", style: {
        color: "gray",
        font: "12pt Calibri, Arial, sans-serif"
    }});
    _fileLabel = new Label({ style: {
        color: "white",
        font: "12pt Calibri, Arial, sans-serif"
    }});

    panel.add([ _projectLabel, symbolThingie, _fileLabel ]);
    panel.layout = function() {
        var d = this.d();

        var x = 0;
        for (var i = 0; i < 2; i++) {
            var width = this.children[i].getPreferredWidth(d.b.h);
            this.children[i].bounds = { x: x, y: 0, width: width, height: d.b.h };
            x += width;
        }

        this.children[2].bounds = { x: x, y: 0, width: d.b.w - d.i.w - x, height: d.b.h };
    }

    _scene.render();
});

// ** {{{ isLoggedIn(userinfo) }}} **
//
// * {{{userinfo}}} is an object containing user specific info (project etc)
//
// Save the users magic project into the session
function isLoggedIn(userinfo) {
    _editSession.username = userinfo.username;
    
    _settings    = new Bespin.Settings.Core();
    _commandLine = new Bespin.CommandLine.Interface($('command'), Bespin.Commands.Editor);
}


// ** {{{ isNotLoggedIn() }}} **
//
// Send the user back to the front page as they aren't logged in.
// The server should stop this from happening, but JUST in case.
function isNotLoggedIn() {
    _.Navigate.home(); // go back
}    

// ** {{{ recalcLayout() }}} **
//
// When a change to the UI is needed due to opening or closing a feature
// (e.g. file view, session view) move the items around
function recalcLayout() {
    var subheader = $("subheader");
    var footer = $("footer");
    var editor = $("editor");
    var files = $("files");
    var collab = $("collab");
    var target = $("target_browsers");

    var move = [ subheader, footer, editor ];

    if (_showFiles) {
        files.style.display = "block";
        move.each(function(item) { item.style.left = "201px" });
    } else {
        files.style.display = "none";
        move.each(function(item) { item.style.left = "0" });
    }

    move.pop();   // editor shouldn't have its right-hand side set

    if (_showCollab) {
        collab.style.display = "block";
        move.each(function(item) { item.style.right = "201px" });
    } else {
        collab.style.display = "none";
        move.each(function(item) { item.style.right = "0" });
    }

    if (_showTarget) {
        target.style.display = "block";
    } else {
        target.style.display = "none";
    }

    doResize();
}

// ** {{{ doResize() }}} **
//
// When a user resizes the window, deal with resizing the canvas and repaint
function doResize() {
    var left = $("subheader").style.left;
    left = (left != "") ? parseInt(left) : 0;
    var right = $("subheader").style.right;
    right = (right != "") ? parseInt(right) : 0;

    Element.writeAttribute($('editor'), {
        width: window.innerWidth - left - right,
        height: window.innerHeight - Bespin.commandlineHeight
    });

    var d = $('status').getDimensions();
    Element.writeAttribute($('projectLabel'), {
        width: d.width,
        height: d.height
    });

    _editor.paint();
}