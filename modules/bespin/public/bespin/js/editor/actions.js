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
if (!Bespin.Editor) Bespin.Editor = {};

// = Actions =
//
// The editor can run various actions. They are defined here and you can add or change them dynamically. Cool huh?
//
// An action mutates the model or editor state in some way. The only way the editor state or model should be manipulated is via
// the execution of actions.
//
// Actions integrate with the undo manager by including instructions for how to undo (and redo) the action. These instructions
// take the form of a hash containing the necessary state for undo/redo. A key "action" corresponds to the function name of the
// action that should be executed to undo or redo the operation and the remaining keys correspond to state necessary to perform
// the action. See below for various examples.

Bespin.Editor.Actions = Class.create({
    initialize: function(editor) {
        this.editor = editor;
        this.ignoreRepaints = false;
    },

    // this is a generic helper method used by various cursor-moving methods
    handleCursorSelection: function(args) {
        if (args.event.shiftKey) {
            if (!this.editor.selection) this.editor.setSelection({ startPos: Bespin.Editor.Utils.copyPos(args.pos) });
            this.editor.setSelection({ startPos: this.editor.selection.startPos, endPos: Bespin.Editor.Utils.copyPos(this.editor.cursorPosition)});
        } else {
            this.editor.setSelection(undefined);
        }
    },

    moveCursorLeft: function(args) {
        // start of the line so move up
        if (_settings.isOn(_settings.get('strictlines')) && (this.editor.cursorPosition.col == 0)) {
            var originalRow = args.pos.row;
            this.moveCursorUp(args);
            if (originalRow > 0) this.moveToLineEnd(args);
            return;
        }
        this.editor.cursorPosition.col = Math.max(0, args.pos.col - 1);
        this.handleCursorSelection(args);
        this.repaint();
    },

    moveCursorRight: function(args) {
        // end of the line, so go to the start of the next line
        if (_settings.isOn(_settings.get('strictlines')) && (this.editor.cursorPosition.col >= this.editor.model.getRowLength(args.pos.row))) {
            var originalRow = args.pos.row;
            this.moveCursorDown(args);
            if (originalRow < this.editor.model.getRowCount() - 1) this.moveToLineStart(args);
            return;
        }

        this.editor.cursorPosition.col = args.pos.col + 1;
        this.handleCursorSelection(args);
        this.repaint();
    },

    moveCursorUp: function(args) {
        this.editor.cursorPosition.row = Math.max(0, args.pos.row - 1);

        if (_settings.isOn(_settings.get('strictlines')) && args.pos.col > this.editor.model.getRowLength(this.editor.cursorPosition.row)) {
            this.handleCursorSelection(args);
            
            args.pos.row -= 1; // one above
            this.moveToLineEnd(args);
        } else {
            this.handleCursorSelection(args);            
        }

        this.repaint();

        args.pos.row = this.editor.cursorPosition.row;
        return args;
    },

    moveCursorDown: function(args) {
        this.editor.cursorPosition.row = Math.min(this.editor.model.getRowCount() - 1, args.pos.row + 1);

        if (_settings.isOn(_settings.get('strictlines')) && args.pos.col > this.editor.model.getRowLength(this.editor.cursorPosition.row)) {
            this.handleCursorSelection(args);
            
            args.pos.row += 1; // one below
            this.moveToLineEnd(args);
        } else {
            this.handleCursorSelection(args);
        }

        this.repaint();

        args.pos.row = this.editor.cursorPosition.row;
        return args;
    },

    moveToLineStart: function(args) {
        this.editor.cursorPosition.col = 0;
        this.handleCursorSelection(args);
        this.repaint();

        args.pos.col = this.editor.cursorPosition.col;
        return args;
    },

    moveToLineEnd: function(args) {
        this.editor.cursorPosition.col = this.editor.model.getRowLength(args.pos.row);
        this.handleCursorSelection(args);
        this.repaint();

        args.pos.col = this.editor.cursorPosition.col;
        return args;
    },

    moveToFileTop: function(args) {
        this.editor.cursorPosition.col = this.editor.cursorPosition.row = 0;

        this.handleCursorSelection(args);
        this.repaint();

        args.pos.col = args.pos.row = 0;

        return args;
    },

    moveToFileBottom: function(args) {
        this.editor.cursorPosition.row = this.editor.model.getRowCount() - 1;
        this.editor.cursorPosition.col = this.editor.model.getRowLength(this.editor.cursorPosition.row);

        this.handleCursorSelection(args);
        this.repaint();

        args.pos.row = this.editor.cursorPosition.row;
        args.pos.col = this.editor.cursorPosition.col;
        return args;
    },

    movePageUp: function(args) {
        this.editor.cursorPosition.row = Math.max(this.editor.ui.firstVisibleRow - this.editor.ui.visibleRows, 0);

        this.handleCursorSelection(args);
        this.repaint();

        return args;
    },

    movePageDown: function(args) {
        this.editor.cursorPosition.row = Math.min(this.editor.cursorPosition.row + this.editor.ui.visibleRows, this.editor.model.getRowCount() - 1);

        this.handleCursorSelection(args);
        this.repaint();

        return args;
    },

    moveWordLeft: function(args) {
        var row = this.editor.model.getRowArray(args.pos.row);

        if (args.pos.col == 0) { // -- at the start to move up and to the end
            var newargs = this.moveCursorUp(args);
            this.moveToLineEnd(newargs);
            return;
        }

        // Short circuit if cursor is ahead of actual spaces in model
        if (row.length < args.pos.col) {
            args = this.moveToLineEnd(args);
        }
        var newcol = args.pos.col;

        // This slurps up trailing spaces
        var wasSpaces = false;
        while (newcol > 0) {
            newcol--;

            var c = row[newcol];
            var charCode = c.charCodeAt(0);
            if (charCode == 32) {
                wasSpaces = true;
            } else {
                newcol++;
                break;
            }
        }

        // This jumps to stop words        
        if (!wasSpaces) {
            while (newcol > 0) {
                newcol--;
                var c = row[newcol];
                var charCode = c.charCodeAt(0);
                if ( (charCode < 65) || (charCode > 122) ) { // if you get to an alpha you are done
                    if (newcol != args.pos.col - 1) newcol++; // right next to a stop char, move back one
                    break;
                }
            }
        }
        
        this.editor.cursorPosition.col = newcol;
        this.handleCursorSelection(args);
        this.repaint();
    },

    moveWordRight: function(args) {
        var row = this.editor.model.getRowArray(args.pos.row);

        if (row.length <= args.pos.col) { // -- at the edge so go to the next line
            this.moveCursorDown(this.moveToLineStart(args));
            return;
        }

        var newcol = args.pos.col;

        // This slurps up leading spaces
        var wasSpaces = false;
        while (newcol < row.length) {
            var c = row[newcol];
            var charCode = c.charCodeAt(0);
            if (charCode == 32) {
                wasSpaces = true;
                newcol++;
            } else {
                break;
            }
        }

        // This jumps to stop words        
        if (!wasSpaces) {        
            while (newcol < row.length) {
                newcol++;

                if (row.length == newcol) { // one more to go
                    this.moveToLineEnd(args);
                    return;
                }

                var c = row[newcol];
                var charCode = c.charCodeAt(0);
            
                if ( (charCode < 65) || (charCode > 122) ) {
                    break;
                }
            }
        }
    
        this.editor.cursorPosition.col = newcol;
        this.handleCursorSelection(args);
        this.repaint();
    },

    undoRedo: function(args) {
        if (! args.event.shiftKey) {    // holding down the shift key causes the undo keystroke to be a redo TODO: move this logic to key handler
            this.undo();
        } else {
            this.redo();
        }
    },

    undo: function() {
        this.editor.undoManager.undo();
    },

    redo: function() {
        this.editor.undoManager.redo();
    },

    selectAll: function(args) {
        // do nothing with an empty doc
        if (this.editor.model.getMaxCols == 0) return;

        args.startPos = { col: 0, row: 0 };
        args.endPos = { col: this.editor.model.getRowLength(this.editor.model.getRowCount() - 1), row: this.editor.model.getRowCount() - 1 };

        this.select(args);
    },

    select: function(args) {
        if (args.startPos) {
            this.editor.setSelection({ startPos: args.startPos, endPos: args.endPos });
            this.editor.moveCursor(args.endPos);
        } else {
            this.editor.setSelection(undefined);
        }
    },

    // TODO: needs undo
    insertTab: function(args) {
        var numberOfCharacters = _settings.get('tabsize') || 4;   // TODO: global needs fixing
        args.newchar = ' ';

        for (var x = 0; x < numberOfCharacters; x++) {
            this.editor.ui.actions.insertCharacter(args);
        }
        this.repaint();
    },

    cutSelection: function(args) {
        this.copySelection(args);
        this.deleteSelection(args);
    },
    
    copySelection: function(args) {
        var selectionObject = this.editor.getSelection();
        if (selectionObject) {
            var selectionText = this.editor.model.getChunk(selectionObject);
            if (selectionText) {
                Bespin.Clipboard.Manual.copy(selectionText);
            }
        }
    },

    deleteSelectionAndInsertChunk: function(args) {
        var oldqueued = args.queued;

        args.queued = true;
        var selection = this.editor.getSelection();
        var chunk = this.deleteSelection(args);
        args.pos = Bespin.Editor.Utils.copyPos(this.editor.cursorPosition);
        var endPos = this.insertChunk(args);

        args.queued = oldqueued;

        // undo/redo
        args.action = "deleteSelectionAndInsertChunk";
        args.selection = selection;
        var redoOperation = args
        var undoArgs = { action: "deleteChunkAndInsertChunkAndSelect", pos: Bespin.Editor.Utils.copyPos(args.pos), endPos: endPos, queued: args.queued, chunk: chunk }
        var undoOperation = undoArgs
        this.editor.undoManager.addUndoOperation(new Bespin.Editor.UndoItem(undoOperation, redoOperation));
    },

    deleteChunkAndInsertChunkAndSelect: function(args) {
        var oldqueued = args.queued;

        args.queued = true;
        this.deleteChunk(args);
        this.insertChunkAndSelect(args);

        args.queued = oldqueued;

        // undo/redo
        args.action = "deleteChunkAndInsertChunkAndSelect";
        var redoOperation = args
        var undoArgs = { action: "deleteSelectionAndInsertChunk", pos: Bespin.Editor.Utils.copyPos(args.pos), queued: args.queued, selection: args.selection }
        var undoOperation = undoArgs
        this.editor.undoManager.addUndoOperation(new Bespin.Editor.UndoItem(undoOperation, redoOperation));
    },

    pasteFromClipboard: function(args) {
        var clipboard = (args.clipboard) ? args.clipboard : Bespin.Clipboard.Manual.data();
        if (clipboard == undefined) return; // darn it clipboard!
        args.chunk = clipboard;
        this.insertChunk(args);
    },

    insertChunk: function(args) {
        if (this.editor.selection) {
            this.deleteSelectionAndInsertChunk(args);
        } else {
            var pos = this.editor.model.insertChunk(Bespin.Editor.Utils.copyPos(this.editor.cursorPosition), args.chunk);
            this.editor.moveCursor(pos);
            this.repaint();

            // undo/redo
            args.action = "insertChunk";
            var redoOperation = args
            var undoArgs = { action: "deleteChunk", pos: Bespin.Editor.Utils.copyPos(args.pos), queued: args.queued, endPos: pos }
            var undoOperation = undoArgs
            this.editor.undoManager.addUndoOperation(new Bespin.Editor.UndoItem(undoOperation, redoOperation));

            return pos;
        }
    },

    deleteChunk: function(args) {
        var chunk = this.editor.model.deleteChunk({ startPos: args.pos, endPos: args.endPos });
        this.editor.moveCursor(args.pos);
        this.repaint();

        // undo/redo
        args.action = "deleteChunk";
        var redoOperation = args
        var undoArgs = { action: "insertChunk", pos: Bespin.Editor.Utils.copyPos(args.pos), queued: args.queued, chunk: chunk }
        var undoOperation = undoArgs
        this.editor.undoManager.addUndoOperation(new Bespin.Editor.UndoItem(undoOperation, redoOperation));
    },

    //deleteLine: function(args) {
    //    this.editor.lines.splice(args.pos.row);
    //    if (args.pos.row >= this.editor.lines.length) this.editor.moveCursor({ row: args.pos.row - 1, col: args.pos.col });
    //    this.repaint();
    //},

    joinLine: function(args) {
        if (args.joinDirection == "up") {
            if (args.pos.row == 0) return;

            var newcol = this.editor.model.getRowLength(args.pos.row - 1);
            this.editor.model.joinRow(args.pos.row - 1);
            this.editor.moveCursor({ row: args.pos.row - 1, col: newcol });
        } else {
            if (args.pos.row >= this.editor.model.getRowCount() - 1) return;

            this.editor.model.joinRow(args.pos.row);
        }

        // undo/redo
        args.action = "joinLine";
        var redoOperation = args
        var undoArgs = { action: "newline", pos: Bespin.Editor.Utils.copyPos(this.editor.cursorPosition), queued: args.queued }
        var undoOperation = undoArgs
        this.editor.undoManager.addUndoOperation(new Bespin.Editor.UndoItem(undoOperation, redoOperation));

        this.repaint();
    },

    killLine: function(args) {
        // select the current row
        this.editor.setSelection({ startPos: { row: args.pos.row, col: 0 }, endPos: { row: args.pos.row + 1, col: 0 } });
        this.cutSelection(args); // cut (will save and redo will work)
    },
    
    deleteSelection: function(args) {
        if (!this.editor.selection) return;
        var selection = this.editor.getSelection();
        var startPos = Bespin.Editor.Utils.copyPos(selection.startPos);
        var chunk = this.editor.model.getChunk(selection);
        this.editor.model.deleteChunk(selection);

        // undo/redo
        args.action = "deleteSelection";
        var redoOperation = args;
        var undoArgs = { action: "insertChunkAndSelect", pos: Bespin.Editor.Utils.copyPos(startPos), queued: args.queued, chunk: chunk }
        var undoOperation = undoArgs
        this.editor.undoManager.addUndoOperation(new Bespin.Editor.UndoItem(undoOperation, redoOperation));

        // setting the selection to undefined has to happen *after* we enqueue the undoOp otherwise replay breaks
        this.editor.setSelection(undefined);
        this.editor.moveCursor(startPos);
        this.repaint();

        return chunk;
    },

    insertChunkAndSelect: function(args) {
        var endPos = this.editor.model.insertChunk(args.pos, args.chunk);

        args.action = "insertChunkAndSelect";
        var redoOperation = args
        var undoArgs = { action: "deleteSelection", pos: Bespin.Editor.Utils.copyPos(endPos), queued: args.queued }
        var undoOperation = undoArgs
        this.editor.undoManager.addUndoOperation(new Bespin.Editor.UndoItem(undoOperation, redoOperation));

        // setting the selection to undefined has to happen *after* we enqueue the undoOp otherwise replay breaks
        this.editor.setSelection({ startPos: args.pos, endPos: endPos });
        this.editor.moveCursor(endPos);
        this.repaint();
    },

    backspace: function(args) {
        if (this.editor.selection) {
            this.deleteSelection(args);
        } else {
            if (args.pos.col > 0) {
                this.editor.cursorPosition.col = Math.max(0, args.pos.col - 1);
                args.pos.col -= 1;
                this.deleteCharacter(args);
            } else {
                args.joinDirection = "up";
                this.joinLine(args);
            }
        }
    },

    deleteKey: function(args) {
        if (this.editor.selection) {
            this.deleteSelection(args);
        } else {
            if (args.pos.col < this.editor.model.getRowLength(args.pos.row)) {
                this.deleteCharacter(args);
            } else {
                args.joinDirection = "down";
                this.joinLine(args);
            }
        }
    },

    deleteCharacter: function(args) {
        if (args.pos.col < this.editor.model.getRowLength(args.pos.row)) {
            var deleted = this.editor.model.deleteCharacters(args.pos, 1);
            this.repaint();

            // undo/redo
            args.action = "deleteCharacter";
            var redoOperation = args
            var undoArgs = { action: "insertCharacter", pos: Bespin.Editor.Utils.copyPos(args.pos), queued: args.queued, newchar: deleted }
            var undoOperation = undoArgs
            this.editor.undoManager.addUndoOperation(new Bespin.Editor.UndoItem(undoOperation, redoOperation));
        }
    },

    newline: function(args) {
        this.editor.model.splitRow(args.pos);
        this.editor.cursorPosition.row += 1;
        this.editor.cursorPosition.col = 0;

        // undo/redo
        args.action = "newline";
        var redoOperation = args
        var undoArgs = { action: "joinLine", joinDirection: "up", pos: Bespin.Editor.Utils.copyPos(this.editor.cursorPosition), queued: args.queued }
        var undoOperation = undoArgs
        this.editor.undoManager.addUndoOperation(new Bespin.Editor.UndoItem(undoOperation, redoOperation));

        this.repaint();
    },

    // it seems kinda silly, but when you have a region selected and you insert a character, I have a separate action that is invoked.
    // this is because it's really two operations: deleting the selected region and then inserting a character. Each of these two
    // actions adds an operation to the undo queue. So I have two choices for
    deleteSelectionAndInsertCharacter: function(args) {
        var oldqueued = args.queued;

        args.queued = true;
        var chunk = this.deleteSelection(args);
        args.pos = Bespin.Editor.Utils.copyPos(this.editor.cursorPosition);
        this.insertCharacter(args);

        args.queued = oldqueued;

        // undo/redo
        args.action = "deleteSelectionAndInsertCharacter";
        var redoOperation = args
        var undoArgs = { action: "deleteCharacterAndInsertChunkAndSelect", pos: Bespin.Editor.Utils.copyPos(args.pos), queued: args.queued, chunk: chunk }
        var undoOperation = undoArgs
        this.editor.undoManager.addUndoOperation(new Bespin.Editor.UndoItem(undoOperation, redoOperation));
    },

    deleteCharacterAndInsertChunkAndSelect: function(args) {
        var oldqueued = args.queued;

        args.queued = true;
        this.deleteCharacter(args);
        this.insertChunkAndSelect(args);

        args.queued = oldqueued;

        // undo/redo
        args.action = "deleteCharacterAndInsertChunkAndSelect";
        var redoOperation = args
        var undoArgs = { action: "deleteSelectionAndInsertCharacter", pos: Bespin.Editor.Utils.copyPos(args.pos), queued: args.queued }
        var undoOperation = undoArgs
        this.editor.undoManager.addUndoOperation(new Bespin.Editor.UndoItem(undoOperation, redoOperation));
    },

    insertCharacter: function(args) {
        if (this.editor.selection) {
            this.deleteSelectionAndInsertCharacter(args);
        } else {
            this.editor.model.insertCharacters(args.pos, args.newchar);
            this.editor.cursorPosition.col += 1;
            this.repaint();

            // undo/redo
            args.action = "insertCharacter";
            var redoOperation = args
            var undoArgs = { action: "deleteCharacter", pos: Bespin.Editor.Utils.copyPos(args.pos), queued: args.queued }
            var undoOperation = undoArgs
            this.editor.undoManager.addUndoOperation(new Bespin.Editor.UndoItem(undoOperation, redoOperation));
        }
    },
    
    moveCursorRowToCenter: function(args) {
        var saveCursorRow = this.editor.cursorPosition.row;
        var halfRows = Math.floor(this.editor.ui.visibleRows / 2);
        if (saveCursorRow > (this.editor.ui.firstVisibleRow + halfRows)) { // bottom half, so move down
            this.editor.cursorPosition.row = this.editor.cursorPosition.row + halfRows;
        } else { // top half, so move up
            this.editor.cursorPosition.row = this.editor.cursorPosition.row - halfRows;
        }
        this.editor.ui.ensureCursorVisible();
        this.editor.cursorPosition.row = saveCursorRow;
    },

    repaint: function() {
        if (!this.ignoreRepaints) {
            this.editor.ui.ensureCursorVisible();
            this.editor.paint();
        }
    }
});
