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

// = Session =
//
// This session module provides functionality that both stores session information
// and handle collaboration.
//
// This module includes:
//
// * {{{ Bespin.Session.EditSession }}}: Wraps a file edit session
// * {{{ Bespin.Session.SyncHelper }}}: Deals with syncing edits back to the server

if (typeof Bespin == "undefined") Bespin = {};
if (!Bespin.Session) Bespin.Session = {};

// ** {{{ EditSession }}} **
//
// EditSession represents a file edit session with the Bespin back-end server. It is responsible for
// sending changes to the server as well as receiving changes from the server and mutating the document
// model with received changes.

Bespin.Session.EditSession = Class.create({
    initialize: function(editor) {
        this.editor = editor;
        this.collaborate = false;
    },
    
    projectForDisplay: function(testProject) {
        var project = testProject || this.project;
        return project;
    },
    
    checkSameFile: function(project, path) {
        return ( (_editSession.project == project) && (_editSession.path == path) );
    },

    startSession: function(project, path, username) {
        this.project = project;
        this.path = path;
        if (!this.username) this.username = username;

        if (this.collaborate) this.syncHelper = new Bespin.Session.SyncHelper(this.editor);
    },

    stopSession: function() {
        this.project = undefined;
        this.path = undefined;

        if (this.collaborate) this.syncHelper.stop();
    }
});

// ** {{{ SyncHelper }}} **
//
// Sends data up to the server (edits), and retrieves updates back and applies them.
// The {{{ EditSession }}} starts and stops this process.

Bespin.Session.SyncHelper = Class.create({
    initialize: function(editor) {
        this.SEND_INTERVAL = 1000;
        this.UPDATE_INTERVAL = 1000;

        this.editor = editor;
        this.editor.undoManager.syncHelper = this;
        this.opQueue = [];
        this.lastOp = 0;
        this.stopped = false;

        var self = this;
        setTimeout(function() { self.processSendQueue() }, self.SEND_INTERVAL );
    },

    retrieveUpdates: function() {
        var self = this;

        // TODO: fix global references
        _server.editAfterActions(_editSession.project, _editSession.path, this.lastOp, function(json) { 

            self.editor.undoManager.syncHelper = undefined; // TODO: document why I do this

            var ops = eval(json);
            this.lastOp += ops.length;

            ops.each(function(op) {
                if (op.username != _editSession.username) { // don't play operations that have been performed by this user
                    self.playOp(op);
                    _showCollabHotCounter = 20;
                }
            });

            if (!_showCollab) {
                $("collaboration").src = (_showCollabHotCounter > 0) ? "images/icn_collab_watching.png" : "images/icn_collab_off.png";
            }

            if (_showCollabHotCounter > 0) _showCollabHotCounter--;

            self.editor.undoManager.syncHelper = self;

            if (!self.stopped) setTimeout(function() { self.retrieveUpdates() }, self.UPDATE_INTERVAL );
        });
    },

    playOp: function(val) {
        var t, ds;
        if (val.redoOp) {
            val.redoOp.queued = undefined;

            this.editor.ui.actions[val.redoOp.action](val.redoOp);
        } else {
            this.editor.ui.actions[val.action](val);
        }
    },

    syncWithServer: function() {
        var self = this;

        _server.editActions(_editSession.project, _editSession.path, function(json) {
            if (json.length > 2) {
                self.editor.undoManager.syncHelper = undefined;

                var ops = eval(json);
                this.lastOp = ops.length;

                self.editor.ui.actions.ignoreRepaints = true;
                ops.each(function(val) {
                    self.playOp(val);
                });
                self.editor.ui.actions.ignoreRepaints = false;
                self.editor.ui.actions.repaint();

                self.editor.undoManager.syncHelper = self;
            }

            setTimeout(function() { self.retrieveUpdates() }, self.UPDATE_INTERVAL );
        });
    },

    stop: function() {
        this.stopped = true;
    },

    processSendQueue: function() {
        if (this.opQueue.length > 0) {
            var sendQueue = this.opQueue.splice(0, this.opQueue.length);
            _server.doAction(_editSession.project, _editSession.path, sendQueue);
        }

        var self = this;
        if (!this.stopped) setTimeout(function() { self.processSendQueue() }, self.SEND_INTERVAL );
    },

    applyEditOperations: function(ops) {
        this.editor.ui.actions.ignoreRepaints = true;

        for (var i = 0; i < ops.length; i++) {
            var op = ops[i];

            // check if this is an editop or an undoop
            if (op.redoOp) {
                op.redo();
            } else {
                this.editor.ui.actions[this.op.action](this.op);
            }
        }

        this.editor.ui.actions.ignoreRepaints = false;
        this.editor.ui.actions.repaint();
    },

    undo: function(op) {
        this.opQueue.push(Object.toJSON({ username: _editSession.username, action: 'undo' }));
    },

    redo: function(op) {
        this.opQueue.push(Object.toJSON({ username: _editSession.username, action: 'redo' }));
    },

    queueUndoOp: function(undoOp) {
        var undoOpJson = {
            username: _editSession.username,
            undoOp: undoOp.undoOp,
            redoOp: undoOp.redoOp
        }
        this.opQueue.push(Object.toJSON(undoOpJson));
    },

    queueSelect: function(selection) {
        this.opQueue.push(Object.toJSON({ username: _editSession.username, action: "select", args: { startPos: (selection) ? selection.startPos : undefined, endPos: (selection) ? selection.endPos : undefined }}));
    }
});
