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

// = Editor =
//
// This is the guts. The metal. The core editor has most of its classes living in here:
//
// * {{{Bespin.Editor.API}}} : The editor API itself
// * {{{Bespin.Editor.UI}}} : Knowledge of the UI pieces of the editor is here, bulk of the code. paints!
// * {{{Bespin.Editor.Scrollbar}}} : The custom scrollbar (to be factored out and use TH scrollbar instead)
// * {{{Bespin.Editor.SelectionHelper}}} : Handle text selection
// * {{{Bespin.Editor.DefaultEditorKeyListener}}} : Key listener operations
// * {{{Bespin.Editor.Rect}}} : Helper to hold a rectangle
// * {{{Bespin.Editor.Events}}} : Helper to hold a rectangle
// * {{{Bespin.Editor.Utils}}} : Blobby utility object to do common things
//
// * {{{Bespin.Editor.Actions}}} : The actions that the editor can do (can be added too) are are in actions.js

// ** {{{ Bespin.Editor.Scrollbar }}} **
//
// some state mgmt. for scrollbars; not a true component
Bespin.Editor.Scrollbar = Class.create({
    HORIZONTAL: "horizontal",
    VERTICAL: "vertical",
    MINIMUM_HANDLE_SIZE: 20,

    initialize: function(ui, orientation, rect, value, min, max, extent) {
        this.ui = ui;
        this.orientation = orientation; // "horizontal" or "vertical"
        this.rect = rect;       // position/size of the scrollbar track
        this.value = value;     // current offset value
        this.min = min;         // minimum offset value
        this.max = max;         // maximum offset value
        this.extent = extent;   // size of the current visible subset

        this.mousedownScreenPoint;    // used for scroll bar dragging tracking; point at which the mousedown first occurred
        this.mousedownValue;          // value at time of scroll drag start
    },

    // return a Rect for the scrollbar handle
    getHandleBounds: function() {
        var sx = (this.isH()) ? this.rect.x : this.rect.y;
        var sw = (this.isH()) ? this.rect.w : this.rect.h;

        var smultiple = this.extent / (this.max + this.extent);
        var asw = smultiple * sw;
        if (asw < this.MINIMUM_HANDLE_SIZE) asw = this.MINIMUM_HANDLE_SIZE;

        sx += (sw - asw) * (this.value / (this.max - this.min));

        return (this.isH()) ? new Bespin.Editor.Rect(Math.floor(sx), this.rect.y, asw, this.rect.h) : new Bespin.Editor.Rect(this.rect.x, sx, this.rect.w, asw);
    },

    isH: function() {
        return (!(this.orientation == this.VERTICAL));
    },

    fixValue: function(value) {
        if (value < this.min) value = this.min;
        if (value > this.max) value = this.max;
        return value;
    },

    onmousewheel: function(e) {
        var axis = Event.axis(e);
        if (this.orientation == this.VERTICAL && axis == this.VERTICAL) {
            this.setValue(this.value + (Event.wheel(e) * this.ui.lineHeight));
        } else if (this.orientation == this.HORIZONTAL && axis == this.HORIZONTAL) {
            this.setValue(this.value + (Event.wheel(e) * this.ui.charWidth));
        }
    },

    onmousedown: function(e) {
        var clientY = e.clientY - this.ui.getTopOffset();
        var clientX = e.clientX - this.ui.getLeftOffset();

        var bar = this.getHandleBounds();
        if (bar.contains({ x: clientX, y: clientY })) {
            this.mousedownScreenPoint = (this.isH()) ? e.screenX : e.screenY;
            this.mousedownValue = this.value;
        } else {
            var p = (this.isH()) ? clientX : clientY;
            var b1 = (this.isH()) ? bar.x : bar.y;
            var b2 = (this.isH()) ? bar.x2 : bar.y2;

            if (p < b1) {
                this.setValue(this.value -= this.extent);
            } else if (p > b2) {
                this.setValue(this.value += this.extent);
            }
        }
    },

    onmouseup: function(e) {
        this.mousedownScreenPoint = null;
        this.mousedownValue = null;
        if (this.valueChanged) this.valueChanged(); // make the UI responsive when the user releases the mouse button (in case arrow no longer hovers over scrollbar)
    },

    onmousemove: function(e) {
        if (this.mousedownScreenPoint) {
            var diff = ((this.isH()) ? e.screenX : e.screenY) - this.mousedownScreenPoint;
            var multiplier = diff / (this.isH() ? this.rect.w : this.rect.h);
            this.setValue(this.mousedownValue + Math.floor(((this.max + this.extent) - this.min) * multiplier));
        }
    },

    setValue: function(value) {
        this.value = this.fixValue(value);
        if (this.valueChanged) this.valueChanged();
    }
});

// ** {{{ Bespin.Editor.Rect }}} **
//
// treat as immutable (pretty please)
Bespin.Editor.Rect = Class.create({
    initialize: function(x, y, w, h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.x2 = x + w;
        this.y2 = y + h;
    },

    // inclusive of bounding lines
    contains: function(point) {
        if (!this.x) return false;
        return ((this.x <= point.x) && ((this.x + this.w) >= point.x) && (this.y <= point.y) && ((this.y + this.h) >= point.y));
    }
});

// ** {{{ Bespin.Editor.SelectionHelper }}} **

Bespin.Editor.SelectionHelper = Class.create({
    initialize: function(editor) {
        this.editor = editor;
    },

    // returns an object with the startCol and endCol of the selection. If the col is -1 on the endPos, the selection goes for the entire line
    // returns undefined if the row has no selection
    getRowSelectionPositions: function(rowIndex) {
        var startCol;
        var endCol;

        var selection = this.editor.getSelection();
        if (!selection) return undefined;
        if ((selection.endPos.row < rowIndex) || (selection.startPos.row > rowIndex)) return undefined;

        startCol = (selection.startPos.row < rowIndex) ? 0 : selection.startPos.col;
        endCol = (selection.endPos.row > rowIndex) ? -1 : selection.endPos.col;

        return { startCol: startCol, endCol: endCol }
    }
});

// ** {{{ Bespin.Editor.Utils }}} **
//
// Mess with positions mainly
Bespin.Editor.Utils = {
    buildArgs: function(oldPos) {
        return { pos: Bespin.Editor.Utils.copyPos(oldPos || _editor.cursorPosition) };    
    },

    changePos: function(args, pos) {
        ar
        return { pos: Bespin.Editor.Utils.copyPos(oldPos || _editor.cursorPosition) };    
    },
    
    copyPos: function(oldPos) {
        return { row: oldPos.row, col: oldPos.col };
    },

    posEquals: function(pos1, pos2) {
        if (pos1 == pos2) return true;
        if (!pos1 || !pos2) return false;
        return (pos1.col == pos2.col) && (pos1.row == pos2.row);
    },

    diffObjects: function(o1, o2) {
        var diffs = {};

        if (!o1 || !o2) return undefined;
        
        for (var key in o1) {
            if (o2[key]) {
                if (o1[key] != o2[key]) {
                    diffs[key] = o1[key] + " => " + o2[key];
                }
            } else {
                diffs[key] = "o1: " + key + " = " + o1[key];
            }
        }

        for (var key2 in o2) {
            if (!o1[key2]) {
                diffs[key2] = "o2: " + key2 + " = " + o2[key2];
            }
        }
        return diffs;
    }
}

// ** {{{ Bespin.Editor.DefaultEditorKeyListener }}} **
// 
// Core key listener to decide which actions to run
Bespin.Editor.DefaultEditorKeyListener = Class.create({
    initialize: function(editor) {
        this.editor = editor;
        this.actions = editor.ui.actions;
        this.skipKeypress = false;

        this.defaultKeyMap = {};

        // Allow for multiple key maps to be defined
        this.keyMap = this.defaultKeyMap;
    },

    bindKey: function(keyCode, metaKey, ctrlKey, altKey, shiftKey, action) {
        this.defaultKeyMap[[keyCode, metaKey, ctrlKey, altKey, shiftKey]] = 
            (typeof action == "string") ?
                function() { 
                    var toFire = Bespin.Events.toFire(action);
                    document.fire(toFire.name, toFire.args);
                } : action.bind(this.actions);
    },

    bindKeyString: function(modifiers, keyCode, action) {
        var ctrlKey = (modifiers.toUpperCase().indexOf("CTRL") != -1);
        var altKey = (modifiers.toUpperCase().indexOf("ALT") != -1);
        var metaKey = (modifiers.toUpperCase().indexOf("META") != -1) || (modifiers.toUpperCase().indexOf("APPLE") != -1) || (modifiers.toUpperCase().indexOf("CMD") != -1);
        var shiftKey = (modifiers.toUpperCase().indexOf("SHIFT") != -1);
        return this.bindKey(keyCode, metaKey, ctrlKey, altKey, shiftKey, action);
    },
    
    bindKeyStringSelectable: function(modifiers, keyCode, action) {
        this.bindKeyString(modifiers, keyCode, action);
        this.bindKeyString("SHIFT " + modifiers, keyCode, action);
    },

    onkeydown: function(e) {
        var handled = _commandLine.handleCommandLineFocus(e);
        if (handled) return false;

        var args = { event: e, pos: Bespin.Editor.Utils.copyPos(this.editor.cursorPosition) }
        this.skipKeypress = false;
        this.returnValue = false;

        var action = this.keyMap[[e.keyCode, e.metaKey, e.ctrlKey, e.altKey, e.shiftKey]];

        var hasAction = false;

        if (typeof action == "function") {
            hasAction = true;
            action(args);
            this.lastAction = action;
        }

        // If a special key is pressed OR if an action is assigned to a given key (e.g. TAB or BACKSPACE)
        if (e.metaKey || e.ctrlKey) {
            this.skipKeypress = true;
            this.returnValue = true;
        }

        // stop going, but allow special strokes to get to the browser
        if (hasAction || !Bespin.Key.passThroughToBrowser(e)) Event.stop(e);
    },

    onkeypress: function(e) {
        var handled = _commandLine.handleCommandLineFocus(e);
        if (handled) return false;
        
        // This is to get around the Firefox bug that happens the first time of jumping between command line and editor
        // Bug https://bugzilla.mozilla.org/show_bug.cgi?id=478686
        if (e.charCode == 'j'.charCodeAt() && e.ctrlKey) {
            Event.stop(e);
            return false;
        }

        if (this.skipKeypress) {
            if (!Bespin.Key.passThroughToBrowser(e)) Event.stop(e);
            return this.returnValue;
        }

        var args = { event: e, pos: Bespin.Editor.Utils.copyPos(this.editor.cursorPosition) };
        var actions = this.editor.ui.actions;

        // Only allow ascii through
        if ((e.charCode >= 32) && (e.charCode <= 126) || e.charCode >= 160) {
            args.newchar = String.fromCharCode(e.charCode);
            actions.insertCharacter(args);
        } else { // Allow user to move with the arrow continuously
            var action = this.keyMap[[e.keyCode, e.metaKey, e.ctrlKey, e.altKey, e.shiftKey]];

            if (this.lastAction == action) {
                delete this.lastAction;
            } else if (typeof action == "function") {
               action(args);
            }
        }

        Event.stop(e);
    }
});


// ** {{{ Bespin.Editor.UI }}} **
//
// Holds the UI. The editor itself, the syntax highlighter, the actions, and more
Bespin.Editor.UI = Class.create({
    initialize: function(editor) {
        this.editor = editor;
        this.syntaxModel = new Bespin.Syntax.Model(editor);
        this.selectionHelper = new Bespin.Editor.SelectionHelper(editor);
        this.actions = new Bespin.Editor.Actions(editor);

        this.toggleCursorFullRepaintCounter = 0;    // tracks how many cursor toggles since the last full repaint

        // these two canvases are used as buffers for the scrollbar images, which are then composited onto the
        // main code view. we could have saved ourselves some misery by just prerendering slices of the scrollbars and
        // combining them like sane people, but... meh
        this.horizontalScrollCanvas = new Element("canvas");
        this.verticalScrollCanvas = new Element("canvas");

        this.GUTTER_WIDTH = 54;
        this.LINE_HEIGHT = 23;
        this.GUTTER_INSETS = { top: 0, left: 6, right: 0, bottom: 6 }
        this.LINE_INSETS = { top: 0, left: 5, right: 0, bottom: 6 }
        this.FALLBACK_CHARACTER_WIDTH = 10;
        this.NIB_WIDTH = 15;
        this.NIB_INSETS = { top: Math.floor(this.NIB_WIDTH / 2),
                            left: Math.floor(this.NIB_WIDTH / 2),
                            right: Math.floor(this.NIB_WIDTH / 2),
                            bottom: Math.floor(this.NIB_WIDTH / 2) }
        this.NIB_ARROW_INSETS = { top: 3, left: 3, right: 3, bottom: 5 }

        this.lineHeight;        // reserved for when line height is calculated dynamically instead of with a constant; set first time a paint occurs
        this.charWidth;         // set first time a paint occurs
        this.visibleRows;       // the number of rows visible in the editor; set each time a paint occurs
        this.firstVisibleRow;   // first row that is visible in the editor; set each time a paint occurs
        this.nibup;             // rect
        this.nibdown;           // rect
        this.nibleft;           // rect
        this.nibright;          // rect

        this.selectMouseDownPos;        // position when the user moused down

        this.xoffset = 0;       // number of pixels to translate the canvas for scrolling
        this.yoffset = 0;

        this.showCursor = true;

        this.overXScrollBar = false;
        this.overYScrollBar = false;
        this.hasFocus = false;

        var source = this.editor.container;
        Event.observe(source, "mousemove", this.handleScrollBars.bindAsEventListener(this));
        Event.observe(source, "mouseout", this.handleScrollBars.bindAsEventListener(this));
        Event.observe(source, "click", this.handleScrollBars.bindAsEventListener(this));
        Event.observe(source, "mousedown", this.handleScrollBars.bindAsEventListener(this));

        Event.observe(source, "mousedown", this.mouseDownSelect.bindAsEventListener(this));
        Event.observe(source, "mousemove", this.mouseMoveSelect.bindAsEventListener(this));
        Event.observe(source, "mouseup", this.mouseUpSelect.bindAsEventListener(this));

        // painting optimization state
        this.lastLineCount = 0;
        this.lastCursorPos = null;
        this.lastxoffset = 0;
        this.lastyoffset = 0;

        this.xscrollbar = new Bespin.Editor.Scrollbar(this, "horizontal");
        this.xscrollbar.valueChanged = function() {
            this.xoffset = -this.xscrollbar.value;
            this.editor.paint();
        }.bindAsEventListener(this);
        Event.observe(window, "mousemove", function(e) {
            this.xscrollbar.onmousemove(e);
        }.bindAsEventListener(this));
        Event.observe(window, "mouseup", function(e) {
            this.xscrollbar.onmouseup(e);
        }.bindAsEventListener(this));
        Event.observe(window, "DOMMouseScroll", function(e) { // Firefox
            this.xscrollbar.onmousewheel(e);
        }.bindAsEventListener(this));
        Event.observe(window, "mousewheel", function(e) { // IE / Opera
            this.xscrollbar.onmousewheel(e);
        }.bindAsEventListener(this));

        this.yscrollbar = new Bespin.Editor.Scrollbar(this, "vertical");
        this.yscrollbar.valueChanged = function() {
            this.yoffset = -this.yscrollbar.value;
            this.editor.paint();
        }.bindAsEventListener(this);
        Event.observe(window, "mousemove", function(e) {
            this.yscrollbar.onmousemove(e);
        }.bindAsEventListener(this));
        Event.observe(window, "mouseup", function(e) {
            this.yscrollbar.onmouseup(e);
        }.bindAsEventListener(this));
        Event.observe(window, "DOMMouseScroll", function(e) { // Firefox
            this.yscrollbar.onmousewheel(e);
        }.bindAsEventListener(this));
        Event.observe(window, "mousewheel", function(e) { // IE / Opera
            this.yscrollbar.onmousewheel(e);
        }.bindAsEventListener(this));

        setTimeout(function() { this.toggleCursor(this) }.bind(this), 250);
    },

    // col is -1 if user clicked in gutter; clicking below last line maps to last line
    convertClientPointToCursorPoint: function(pos) {
        var x, y;

        if (pos.y >= (this.lineHeight * this.editor.model.getRowCount())) {
            y = this.editor.model.getRowCount() - 1;
        } else {
            var ty = pos.y;
            y = Math.floor(ty / this.lineHeight);
        }

        if (pos.x <= (this.GUTTER_WIDTH + this.LINE_INSETS.left)) {
            x = -1;
        } else {
            var tx = pos.x - this.GUTTER_WIDTH - this.LINE_INSETS.left;
            x = Math.floor(tx / this.charWidth);
            
            // With striclines turned on, don't select past the end of the line
            if (_settings.isOn(_settings.get('strictlines'))) {
                var maxcol = this.editor.model.getRowLength(y);
            
                if (x >= maxcol) {
                    x = this.editor.model.getRowLength(y);
                }
            }
        }
        
        return { col: x, row: y };
    },

    mouseDownSelect: function(e) {
        var clientY = e.clientY - this.getTopOffset();
        var clientX = e.clientX - this.getLeftOffset();

        if (this.overXScrollBar || this.overYScrollBar) return;

        if (e.shiftKey) {
            this.selectMouseDownPos = (this.editor.selection) ? this.editor.selection.startPos : this.editor.cursorPosition;
            this.setSelection(e);
        } else {
            var point = { x: clientX, y: clientY };
            point.x += Math.abs(this.xoffset);
            point.y += Math.abs(this.yoffset);

            if ((this.xscrollbar.rect.contains(point)) || (this.yscrollbar.rect.contains(point))) return;
            this.selectMouseDownPos = this.convertClientPointToCursorPoint(point);
        }
    },

    mouseMoveSelect: function(e) {
        this.setSelection(e);
    },

    mouseUpSelect: function(e) {
        this.setSelection(e);
        this.selectMouseDownPos = undefined;
    },

    setSelection: function(e) {
        var clientY = e.clientY - this.getTopOffset();
        var clientX = e.clientX - this.getLeftOffset();

        if (!this.selectMouseDownPos) return;

        var down = Bespin.Editor.Utils.copyPos(this.selectMouseDownPos);

        var point = { x: clientX, y: clientY };
        point.x += Math.abs(this.xoffset);
        point.y += Math.abs(this.yoffset);
        var up = this.convertClientPointToCursorPoint(point);

        if (down.col == -1) down.col = 0;
        if (up.col == -1) up.col = 0;

        if (!Bespin.Editor.Utils.posEquals(down, up)) {
            this.editor.setSelection({ startPos: down, endPos: up });
        } else {
            if (e.detail == 1) {
                this.editor.setSelection(undefined);
            } else if (e.detail == 2) {
                var row = this.editor.model.rows[down.row];
                var cursorAt = row[down.col];
                if (!cursorAt || cursorAt.charAt(0) == ' ') { // empty space
                    // For now, don't select anything, but think about copying Textmate and grabbing around it
                } else {
                    var startPos = up = this.editor.model.findBefore(down.row, down.col);
                    
                    var endPos = this.editor.model.findAfter(down.row, down.col);
                    
                    this.editor.setSelection({ startPos: startPos, endPos: endPos });
                }
            } else if (e.detail > 2) {
                // select the line
                this.editor.setSelection({ startPos: { row: down.row, col: 0 }, endPos: { row: down.row + 1, col: 0 } });
            }
        }

        this.editor.moveCursor(up);
        this.editor.paint();
    },

    toggleCursor: function(ui) {
        ui.showCursor = !ui.showCursor;

        if (++this.toggleCursorFullRepaintCounter > 0) {
            this.toggleCursorFullRepaintCounter = 0;
            ui.editor.paint(true);
        } else {
            ui.editor.paint();
        }

        setTimeout(function() { ui.toggleCursor(ui) }, 250);
    },

    ensureCursorVisible: function() {
        if ((!this.lineHeight) || (!this.charWidth)) return;    // can't do much without these

        var y = this.lineHeight * this.editor.cursorPosition.row;
        var x = this.charWidth * this.editor.cursorPosition.col;

        var cheight = this.getHeight();
        var cwidth = this.getWidth() - this.GUTTER_WIDTH;

        if (Math.abs(this.yoffset) > y) {               // current row before top-most visible row
            this.yoffset = -y;
        } else if ((Math.abs(this.yoffset) + cheight) < (y + this.lineHeight)) {       // current row after bottom-most visible row
            this.yoffset = -((y + this.lineHeight) - cheight);
        }

        if (Math.abs(this.xoffset) > x) {               // current col before left-most visible col
            this.xoffset = -x;
        } else if ((Math.abs(this.xoffset) + cwidth) < (x + (this.charWidth * 2))) { // current col after right-most visible col
            this.xoffset = -((x + (this.charWidth * 2)) - cwidth);
        }
    },

    handleFocus: function(e) {
        this.editor.model.clear();
        this.editor.model.insertCharacters({ row: 0, col: 0}, e.type);
    },
    
    handleScrollBars: function(e) {
        var clientY = e.clientY - this.getTopOffset();
        var clientX = e.clientX - this.getLeftOffset();

        var oldX = this.overXScrollBar;
        var oldY = this.overYScrollBar;
        var scrolled = false;

        var w = this.editor.container.clientWidth;
        var h = this.editor.container.clientHeight;
        var sx = w - this.NIB_WIDTH - this.NIB_INSETS.right;    // x start of the vert. scroll bar
        var sy = h - this.NIB_WIDTH - this.NIB_INSETS.bottom;   // y start of the hor. scroll bar

        var p = { x: clientX, y:clientY };

        if (e.type == "mousedown") {
            // dispatch to the scrollbars
            if ((this.xscrollbar) && (this.xscrollbar.rect.contains(p))) {
                this.xscrollbar.onmousedown(e);
            } else if ((this.yscrollbar) && (this.yscrollbar.rect.contains(p))) {
                this.yscrollbar.onmousedown(e);
            }
        }

        if (e.type == "mouseout") {
            this.overXScrollBar = false;
            this.overYScrollBar = false;
        }

        if ((e.type == "mousemove") || (e.type == "click")) {
            this.overYScrollBar = p.x > sx;
            this.overXScrollBar = p.y > sy;
        }

        if (e.type == "click") {
            if (Event.isLeftClick(e)) {

                var button;
                if (this.nibup.contains(p)) {
                    button = "up";
                } else if (this.nibdown.contains(p)) {
                    button = "down";
                } else if (this.nibleft.contains(p)) {
                    button = "left";
                } else if (this.nibright.contains(p)) {
                    button = "right";
                }

                if (button == "up") {
                    this.yoffset += this.lineHeight;
                    scrolled = true;
                } else if (button == "down") {
                    this.yoffset -= this.lineHeight;
                    scrolled = true;
                } else if (button == "left") {
                    this.xoffset += this.charWidth * 2;
                    scrolled = true;
                } else if (button == "right") {
                    this.xoffset -= this.charWidth * 2;
                    scrolled = true;
                }
            }
        }

        if ((oldX != this.overXScrollBar) || (oldY != this.overYScrollBar) || scrolled) this.editor.paint();
    },

    installKeyListener: function(listener) {
        var Key = Bespin.Key; // alias

        if (this.oldkeydown) Event.stopObserving(document, "keydown", this.oldkeydown);
        if (this.oldkeypress) Event.stopObserving(document, "keypress", this.oldkeypress);

        this.oldkeydown = listener.onkeydown.bindAsEventListener(listener);
        this.oldkeypress = listener.onkeypress.bindAsEventListener(listener);

        Event.observe(document, "keydown", this.oldkeydown);
        Event.observe(document, "keypress", this.oldkeypress);

        // Modifiers, Key, Action
        
        listener.bindKeyStringSelectable("", Key.ARROW_LEFT, this.actions.moveCursorLeft);
        listener.bindKeyStringSelectable("", Key.ARROW_RIGHT, this.actions.moveCursorRight);
        listener.bindKeyStringSelectable("", Key.ARROW_UP, this.actions.moveCursorUp);
        listener.bindKeyStringSelectable("", Key.ARROW_DOWN, this.actions.moveCursorDown);

        listener.bindKeyStringSelectable("ALT", Key.ARROW_LEFT, this.actions.moveWordLeft);
        listener.bindKeyStringSelectable("ALT", Key.ARROW_RIGHT, this.actions.moveWordRight);

        listener.bindKeyStringSelectable("", Key.HOME, this.actions.moveToLineStart);
        listener.bindKeyStringSelectable("APPLE", Key.ARROW_LEFT, this.actions.moveToLineStart);
        listener.bindKeyStringSelectable("", Key.END, this.actions.moveToLineEnd);
        listener.bindKeyStringSelectable("APPLE", Key.ARROW_RIGHT, this.actions.moveToLineEnd);

        listener.bindKeyString("CTRL", Key.K, this.actions.killLine);
        listener.bindKeyString("CTRL", Key.L, this.actions.moveCursorRowToCenter);

        listener.bindKeyString("", Key.BACKSPACE, this.actions.backspace);
        listener.bindKeyString("", Key.DELETE, this.actions.deleteKey);
        listener.bindKeyString("", Key.ENTER, this.actions.newline);
        listener.bindKeyString("", Key.TAB, this.actions.insertTab);

        listener.bindKeyString("APPLE", Key.A, this.actions.selectAll);
        listener.bindKeyString("CTRL", Key.A, this.actions.selectAll);

        listener.bindKeyString("APPLE", Key.Z, this.actions.undoRedo);
        listener.bindKeyString("CTRL", Key.Z, this.actions.undoRedo);

        // listener.bindKeyString("APPLE", Key.C, this.actions.copySelection);
        // listener.bindKeyString("CTRL", Key.C, this.actions.copySelection);
        // 
        // listener.bindKeyString("APPLE", Key.V, this.actions.pasteFromClipboard);
        // listener.bindKeyString("CTRL", Key.V, this.actions.pasteFromClipboard);
        // 
        // listener.bindKeyString("APPLE", Key.X, this.actions.cutSelection);
        // listener.bindKeyString("CTRL", Key.X, this.actions.cutSelection);

        listener.bindKeyStringSelectable("APPLE", Key.ARROW_UP, this.actions.moveToFileTop);
        listener.bindKeyStringSelectable("APPLE", Key.ARROW_DOWN, this.actions.moveToFileBottom);
        
        listener.bindKeyStringSelectable("", Key.PAGE_UP, this.actions.movePageUp);
        listener.bindKeyStringSelectable("", Key.PAGE_DOWN, this.actions.movePageDown);

        // Other key bindings can be found in commands themselves.
        // For example, this:
        // listener.bindKeyString("CTRL SHIFT", Key.N, "bespin:editor:newfile");
        // has been moved to the 'newfile' command withKey
    },

    getWidth: function() {
        var element = this.editor.canvas.parentNode;
        return parseInt(element.getAttribute("width"));
    },

    getHeight: function() {
        var element = this.editor.canvas.parentNode;
        return parseInt(element.getAttribute("height"));
    },

    getTopOffset: function() {
        return this.editor.canvas.parentNode.offsetTop;
    },

    getLeftOffset: function() {
        return this.editor.canvas.parentNode.offsetLeft;
    },

    getCharWidth: function(ctx) {
        if (ctx.measureText) {
            return ctx.measureText("M").width;
        } else {
            return this.FALLBACK_CHARACTER_WIDTH;
        }
    },

    getLineHeight: function(ctx) {
        var lh = -1;
        if (ctx.measureText) {
            var t = ctx.measureText("M");
            if (t.ascent) lh = Math.floor(t.ascent * 2.8);
        }
        if (lh == -1) lh = this.LINE_HEIGHT;
        return lh;
    },

    // ** {{{ paint }}} **
    //
    // This is where the editor is painted from head to toe. The optional "fullRefresh" argument triggers a complete repaint
    // of the editor canvas; otherwise, pitiful tricks are used to draw as little as possible.
    paint: function(ctx, fullRefresh) {
        // DECLARE VARIABLES

        // these are convenience references so we don't have to type so much
        var c = $(this.editor.canvas);
        var theme = this.editor.theme;
        var ed = this.editor;

        // these are commonly used throughout the rendering process so are defined up here to make it clear they are shared
        var x, y;
        var cy;
        var currentLine;
        var lastLineToRender;

        var Rect = Bespin.Editor.Rect;


        // SETUP STATE

        var refreshCanvas = fullRefresh;        // if the user explicitly requests a full refresh, give it to 'em

        if (!refreshCanvas) refreshCanvas = (this.selectMouseDownPos);

        if (!refreshCanvas) refreshCanvas = (this.lastLineCount != ed.model.getRowCount());  // if the line count has changed, full refresh

        this.lastLineCount = ed.model.getRowCount();        // save the number of lines for the next time paint

        // get the line and character metrics; calculated for each paint because this value can change at run-time
        ctx.font = theme.lineNumberFont;
        this.charWidth = this.getCharWidth(ctx);
        this.lineHeight = this.getLineHeight(ctx);

        // cwidth and cheight are set to the dimensions of the parent node of the canvas element; we'll resize the canvas element
        // itself a little bit later in this function
        var cwidth = this.getWidth();
        var cheight = this.getHeight();

        var virtualheight = this.lineHeight * ed.model.getRowCount();    // full height based on content
        var virtualwidth = this.charWidth * (Math.max(ed.model.getMaxCols(), ed.cursorPosition.col) + 2);       // full width based on content plus a little padding

        // adjust the scrolling offsets if necessary; negative values are good, indicate scrolling down or to the right (we look for overflows on these later on)
        // positive values are bad; they indicate scrolling up past the first line or to the left past the first column
        if (this.xoffset > 0) this.xoffset = 0;
        if (this.yoffset > 0) this.yoffset = 0;

        // these next two blocks make sure we don't scroll too far in either the x or y axis
        if (this.xoffset < 0) {
            if ((Math.abs(this.xoffset)) > (virtualwidth - (cwidth - this.GUTTER_WIDTH))) this.xoffset = (cwidth - this.GUTTER_WIDTH) - virtualwidth;
        }
        if (this.yoffset < 0) {
            if ((Math.abs(this.yoffset)) > (virtualheight - cheight)) this.yoffset = cheight - virtualheight;
        }

        // if the current scrolled positions are different than the scroll positions we used for the last paint, refresh the entire canvas
        if ((this.xoffset != this.lastxoffset) || (this.yoffset != this.lastyoffset)) {
            refreshCanvas = true;
            this.lastxoffset = this.xoffset;
            this.lastyoffset = this.yoffset;
        }

        // these are boolean values indicating whether the x and y (i.e., horizontal or vertical) scroll bars are visible
        var xscroll = ((cwidth - this.GUTTER_WIDTH) < virtualwidth);
        var yscroll = (cheight < virtualheight);

        // the scroll bars are rendered off-screen into their own canvas instances; these values are used in two ways as part of
        // this process:
        //   1. the x position of the vertical scroll bar image when painted onto the canvas and the y position of the horizontal
        //      scroll bar image (both images span 100% of the width/height in the other dimension)
        //   2. the amount * -1 to translate the off-screen canvases used by the scrollbars; this lets us flip back to rendering
        //      the scroll bars directly on the canvas with relative ease (by omitted the translations and passing in the main context
        //      reference instead of the off-screen canvas context)
        var verticalx = cwidth - this.NIB_WIDTH - this.NIB_INSETS.right - 2;
        var horizontaly = cheight - this.NIB_WIDTH - this.NIB_INSETS.bottom - 2;

        // these are boolean values that indicate whether special little "nibs" should be displayed indicating more content to the
        // left, right, top, or bottom
        var showLeftScrollNib = (xscroll && (this.xoffset != 0));
        var showRightScrollNib = (xscroll && (this.xoffset > ((cwidth - this.GUTTER_WIDTH) - virtualwidth)));
        var showUpScrollNib = (yscroll && (this.yoffset != 0));
        var showDownScrollNib = (yscroll && (this.yoffset > (cheight - virtualheight)));

        // check and see if the canvas is the same size as its immediate parent in the DOM; if not, resize the canvas
        if (((c.readAttribute("width")) != cwidth) || (c.readAttribute("height") != cheight)) {
            refreshCanvas = true;   // if the canvas changes size, we'll need a full repaint
            c.writeAttribute({ width: cwidth, height: cheight });
        }

        // IF YOU WANT TO FORCE A COMPLETE REPAINT OF THE CANVAS ON EVERY PAINT, UNCOMMENT THE FOLLOWING LINE:
        //refreshCanvas = true;

        // START RENDERING

        // if we're not doing a full repaint, work out which rows are "dirty" and need to be repainted
        if (!refreshCanvas) {
            var dirty = ed.model.getDirtyRows();

            // if the cursor has changed rows since the last paint, consider the previous row dirty
            if ((this.lastCursorPos) && (this.lastCursorPos.row != ed.cursorPosition.row)) dirty[this.lastCursorPos.row] = true;

            // we always repaint the current line
            dirty[ed.cursorPosition.row] = true;
        }

        // save this state for the next paint attempt (see above for usage)
        this.lastCursorPos = Bespin.Editor.Utils.copyPos(ed.cursorPosition);

        // if we're doing a full repaint...
        if (refreshCanvas) {
            // ...paint the background color over the whole canvas and...
            ctx.fillStyle = theme.backgroundStyle;
            ctx.fillRect(0, 0, c.width, c.height);

            // ...paint the gutter 
            ctx.fillStyle = theme.gutterStyle;
            ctx.fillRect(0, 0, this.GUTTER_WIDTH, c.height);
        }

        // translate the canvas based on the scrollbar position; for now, just translate the vertical axis
        ctx.save(); // take snapshot of current context state so we can roll back later on
        ctx.translate(0, this.yoffset);

        // only paint those lines that can be visible
        this.visibleRows = Math.ceil(cheight / this.lineHeight);
        this.firstVisibleRow = Math.floor(Math.abs(this.yoffset / this.lineHeight));
        lastLineToRender = this.firstVisibleRow + this.visibleRows;
        if (lastLineToRender > (ed.model.getRowCount() - 1)) lastLineToRender = ed.model.getRowCount() - 1;

        // paint the line numbers
        if (refreshCanvas) {
            y = (this.lineHeight * this.firstVisibleRow);
            for (currentLine = this.firstVisibleRow; currentLine <= lastLineToRender; currentLine++) {
                x = this.GUTTER_INSETS.left;
                cy = y + (this.lineHeight - this.LINE_INSETS.bottom);

                ctx.fillStyle = theme.lineNumberColor;
                ctx.font = this.editor.theme.lineNumberFont;
                ctx.fillText(currentLine + 1, x, cy);

                y += this.lineHeight;
            }
        }

        // and now we're ready to translate the horizontal axis; while we're at it, we'll setup a clip to prevent any drawing outside
        // of code editor region itself (protecting the gutter). this clip is important to prevent text from bleeding into the gutter.
        ctx.save();
        ctx.beginPath();
        ctx.rect(this.GUTTER_WIDTH, -this.yoffset, cwidth - this.GUTTER_WIDTH, cheight);
        ctx.closePath();
        ctx.translate(this.xoffset, 0);
        ctx.clip();

        // calculate the first and last visible columns on the screen; these values will be used to try and avoid painting text
        // that the user can't actually see
        var firstColumn = Math.floor(Math.abs(this.xoffset / this.charWidth));
        var lastColumn = firstColumn + (Math.ceil((cwidth - this.GUTTER_WIDTH) / this.charWidth));

        // paint the line content and zebra stripes
        y = (this.lineHeight * this.firstVisibleRow);
        var cc; // the starting column of the current region in the region render loop below
        var ce; // the ending column in the same loop
        var ri; // counter variable used for the same loop
        var regionlen;  // length of the text in the region; used in the same loop
        var tx, tw;
        for (currentLine = this.firstVisibleRow; currentLine <= lastLineToRender; currentLine++) {
            x = this.GUTTER_WIDTH;

            // if we aren't repainting the entire canvas...
            if (!refreshCanvas) {
                // ...don't bother painting the line unless it is "dirty" (see above for dirty checking)
                if (!dirty[currentLine]) {
                    y += this.lineHeight;
                    continue;
                }

                // setup a clip for the current line only; this makes drawing just that piece of the scrollbar easy
                ctx.save();
                ctx.beginPath();
                ctx.rect(x + (Math.abs(this.xoffset)), y, cwidth, this.lineHeight);
                ctx.closePath();
                ctx.clip();

                if ((currentLine % 2) == 1) { // only repaint the line background if the zebra stripe won't be painted into it
                    ctx.fillStyle = theme.backgroundStyle;
                    ctx.fillRect(x + (Math.abs(this.xoffset)), y, cwidth, this.lineHeight);
                }
            }

            if ((currentLine % 2) == 0) {
                ctx.fillStyle = theme.zebraStripeColor;
                ctx.fillRect(x + (Math.abs(this.xoffset)), y, cwidth, this.lineHeight);
            }

            x += this.LINE_INSETS.left;
            cy = y + (this.lineHeight - this.LINE_INSETS.bottom);

            // paint the selection bar if the line has selections
            var selections = this.selectionHelper.getRowSelectionPositions(currentLine);
            if (selections) {
                tx = x + (selections.startCol * this.charWidth);
                tw = (selections.endCol == -1) ? (lastColumn - firstColumn) * this.charWidth : (selections.endCol - selections.startCol) * this.charWidth;
                ctx.fillStyle = theme.editorSelectedTextBackground;
                ctx.fillRect(tx, y, tw, this.lineHeight);
            }

            // the following two chunks of code do the same thing; only one should be uncommented at a time

            // CHUNK 1: this code just renders the line with white text and is for testing
//            ctx.fillStyle = "white";
//            ctx.fillText(this.editor.model.getRowArray(currentLine).join(""), x, cy);

            // CHUNK 2: this code uses new the SyntaxModel API to attempt to render a line with fewer passes than the color helper API
            var lineInfo = this.syntaxModel.getSyntaxStyles(currentLine, this.editor.language);
            
            for (ri = 0; ri < lineInfo.regions.length; ri++) {
                var styleInfo = lineInfo.regions[ri];

                for (var style in styleInfo) {
                    if (!styleInfo.hasOwnProperty(style)) continue;

                    var thisLine = "";

                    var styleArray = styleInfo[style];
                    var currentColumn = 0; // current column, inclusive
                    for (var si = 0; si < styleArray.length; si++) {
                        var range = styleArray[si];
                        for ( ; currentColumn < range.start; currentColumn++) thisLine += " ";
                        thisLine += lineInfo.text.substring(range.start, range.stop);
                        currentColumn = range.stop;
                    }

                    ctx.fillStyle = this.editor.theme[style] || "white";
                    ctx.font = this.editor.theme.lineNumberFont;
                    ctx.fillText(thisLine, x, cy);
                }
            }

            if (!refreshCanvas) {
                ctx.drawImage(this.verticalScrollCanvas, verticalx + Math.abs(this.xoffset), Math.abs(this.yoffset));
                ctx.restore();
            }

            y += this.lineHeight;
        }

        // paint the cursor
        if (this.editor.focus) {
            if (this.showCursor) {
                if (ed.theme.cursorType == "underline") {
                    x = this.GUTTER_WIDTH + this.LINE_INSETS.left + ed.cursorPosition.col * this.charWidth;
                    y = (ed.cursorPosition.row * this.lineHeight) + (this.lineHeight - 5);
                    ctx.fillStyle = ed.theme.cursorStyle;
                    ctx.fillRect(x, y, this.charWidth, 3);
                } else {
                    x = this.GUTTER_WIDTH + this.LINE_INSETS.left + ed.cursorPosition.col * this.charWidth;
                    y = (ed.cursorPosition.row * this.lineHeight);
                    ctx.fillStyle = ed.theme.cursorStyle;
                    ctx.fillRect(x, y, 1, this.lineHeight);
                }
            }
        } else {
            x = this.GUTTER_WIDTH + this.LINE_INSETS.left + ed.cursorPosition.col * this.charWidth;
            y = (ed.cursorPosition.row * this.lineHeight);

            ctx.fillStyle = ed.theme.unfocusedCursorFillStyle;
            ctx.strokeStyle = ed.theme.unfocusedCursorStrokeStyle;
            ctx.fillRect(x, y, this.charWidth, this.lineHeight);
            ctx.strokeRect(x, y, this.charWidth, this.lineHeight);
        }

        // scroll bars - x axis
        ctx.restore();

        // scrollbars - y axis
        ctx.restore();

        // paint scroll bars unless we don't need to :-)
        if (!refreshCanvas) return;

        // temporary disable of scrollbars
        //if (this.xscrollbar.rect) return;

        if (this.horizontalScrollCanvas.width != cwidth) this.horizontalScrollCanvas.width = cwidth;
        if (this.horizontalScrollCanvas.height != this.NIB_WIDTH + 4) this.horizontalScrollCanvas.height = this.NIB_WIDTH + 4;

        if (this.verticalScrollCanvas.height != cheight) this.verticalScrollCanvas.height = cheight;
        if (this.verticalScrollCanvas.width != this.NIB_WIDTH + 4) this.verticalScrollCanvas.width = this.NIB_WIDTH + 4;

        var hctx = this.horizontalScrollCanvas.getContext("2d");
        hctx.clearRect(0, 0, this.horizontalScrollCanvas.width, this.horizontalScrollCanvas.height);
        hctx.save();

        var vctx = this.verticalScrollCanvas.getContext("2d");
        vctx.clearRect(0, 0, this.verticalScrollCanvas.width, this.verticalScrollCanvas.height);
        vctx.save();

        var ythemes = (this.overYScrollBar) || (this.yscrollbar.mousedownValue != null) ?
                      { n: ed.theme.fullNibStyle, a: ed.theme.fullNibArrowStyle, s: ed.theme.fullNibStrokeStyle } :
                      { n: ed.theme.partialNibStyle, a: ed.theme.partialNibArrowStyle, s: ed.theme.partialNibStrokeStyle };
        var xthemes = (this.overXScrollBar) || (this.xscrollbar.mousedownValue != null) ?
                      { n: ed.theme.fullNibStyle, a: ed.theme.fullNibArrowStyle, s: ed.theme.fullNibStrokeStyle } :
                      { n: ed.theme.partialNibStyle, a: ed.theme.partialNibArrowStyle, s: ed.theme.partialNibStrokeStyle };

        var midpoint = Math.floor(this.NIB_WIDTH / 2);

        this.nibup = new Rect(cwidth - this.NIB_INSETS.right - this.NIB_WIDTH,
                this.NIB_INSETS.top, this.NIB_WIDTH, this.NIB_WIDTH);

        this.nibdown = new Rect(cwidth - this.NIB_INSETS.right - this.NIB_WIDTH,
                cheight - (this.NIB_WIDTH * 2) - (this.NIB_INSETS.bottom * 2),
                this.NIB_INSETS.top,
                this.NIB_WIDTH, this.NIB_WIDTH);

        this.nibleft = new Rect(this.GUTTER_WIDTH + this.NIB_INSETS.left, cheight - this.NIB_INSETS.bottom - this.NIB_WIDTH,
                this.NIB_WIDTH, this.NIB_WIDTH);

        this.nibright = new Rect(cwidth - (this.NIB_INSETS.right * 2) - (this.NIB_WIDTH * 2),
                cheight - this.NIB_INSETS.bottom - this.NIB_WIDTH,
                this.NIB_WIDTH, this.NIB_WIDTH);

        vctx.translate(-verticalx, 0);
        hctx.translate(0, -horizontaly);

        if (xscroll && ((this.overXScrollBar) || (this.xscrollbar.mousedownValue != null))) {
            hctx.save();

            hctx.beginPath();
            hctx.rect(this.nibleft.x + midpoint + 2, 0, this.nibright.x - this.nibleft.x - 1, cheight); // y points don't matter
            hctx.closePath();
            hctx.clip();

            hctx.fillStyle = ed.theme.scrollTrackFillStyle;
            hctx.fillRect(this.nibleft.x, this.nibleft.y - 1, this.nibright.x2 - this.nibleft.x, this.nibleft.h + 1);

            hctx.strokeStyle = ed.theme.scrollTrackStrokeStyle;
            hctx.strokeRect(this.nibleft.x, this.nibleft.y - 1, this.nibright.x2 - this.nibleft.x, this.nibleft.h + 1);

            hctx.restore();
        }

        if (yscroll && ((this.overYScrollBar) || (this.yscrollbar.mousedownValue != null))) {
            vctx.save();

            vctx.beginPath();
            vctx.rect(0, this.nibup.y + midpoint + 2, cwidth, this.nibdown.y - this.nibup.y - 1); // x points don't matter
            vctx.closePath();
            vctx.clip();

            vctx.fillStyle = ed.theme.scrollTrackFillStyle;
            vctx.fillRect(this.nibup.x - 1, this.nibup.y, this.nibup.w + 1, this.nibdown.y2 - this.nibup.y);

            vctx.strokeStyle = ed.theme.scrollTrackStrokeStyle;
            vctx.strokeRect(this.nibup.x - 1, this.nibup.y, this.nibup.w + 1, this.nibdown.y2 - this.nibup.y);

            vctx.restore();
        }

        if (yscroll) {
            // up arrow
            if ((showUpScrollNib) || (this.overYScrollBar) || (this.yscrollbar.mousedownValue != null)) {
                vctx.save();
                vctx.translate(this.nibup.x + midpoint, this.nibup.y + midpoint);
                this.paintNib(vctx, ythemes.n, ythemes.a, ythemes.s);
                vctx.restore();
            }

            // down arrow
            if ((showDownScrollNib) || (this.overYScrollBar) || (this.yscrollbar.mousedownValue != null)) {
                vctx.save();
                vctx.translate(this.nibdown.x + midpoint, this.nibdown.y + midpoint);
                vctx.rotate(Math.PI);
                this.paintNib(vctx, ythemes.n, ythemes.a, ythemes.s);
                vctx.restore();
            }
        }

        if (xscroll) {
            // left arrow
            if ((showLeftScrollNib) || (this.overXScrollBar) || (this.xscrollbar.mousedownValue != null)) {
                hctx.save();
                hctx.translate(this.nibleft.x + midpoint, this.nibleft.y + midpoint);
                hctx.rotate(Math.PI * 1.5);
                this.paintNib(hctx, xthemes.n, xthemes.a, xthemes.s);
                hctx.restore();
            }

            // right arrow
            if ((showRightScrollNib) || (this.overXScrollBar) || (this.xscrollbar.mousedownValue != null)) {
                hctx.save();
                hctx.translate(this.nibright.x + midpoint, this.nibright.y + midpoint);
                hctx.rotate(Math.PI * 0.5);
                this.paintNib(hctx, xthemes.n, xthemes.a, xthemes.s);
                hctx.restore();
            }
        }

        // the bar
        var sx = this.nibleft.x2 + 4;
        var sw = this.nibright.x - this.nibleft.x2 - 9;
        this.xscrollbar.rect = new Rect(sx, this.nibleft.y - 1, sw, this.nibleft.h + 1);
        this.xscrollbar.value = -this.xoffset;
        this.xscrollbar.min = 0;
        this.xscrollbar.max = virtualwidth - (cwidth - this.GUTTER_WIDTH);
        this.xscrollbar.extent = cwidth - this.GUTTER_WIDTH;

        if (xscroll) {
            var fullonxbar = (((this.overXScrollBar) && (virtualwidth > cwidth)) || ((this.xscrollbar) && (this.xscrollbar.mousedownValue != null)));
            if (!fullonxbar) hctx.globalAlpha = 0.3;
            this.paintScrollbar(hctx, this.xscrollbar);
            hctx.globalAlpha = 1.0;
        }

        var sy = this.nibup.y2 + 4;
        var sh = this.nibdown.y - this.nibup.y2 - 9;
        this.yscrollbar.rect = new Rect(this.nibup.x - 1, sy, this.nibup.w + 1, sh);
        this.yscrollbar.value = -this.yoffset;
        this.yscrollbar.min = 0;
        this.yscrollbar.max = virtualheight - cheight;
        this.yscrollbar.extent = cheight;

        if (yscroll) {
            var fullonybar = ((this.overYScrollBar) && (virtualheight > cheight)) || ((this.yscrollbar) && (this.yscrollbar.mousedownValue != null));
            if (!fullonybar) vctx.globalAlpha = 0.3;
            this.paintScrollbar(vctx, this.yscrollbar);
            vctx.globalAlpha = 1;
        }

        // composite the scrollbars
        ctx.drawImage(this.verticalScrollCanvas, verticalx, 0);
        ctx.drawImage(this.horizontalScrollCanvas, 0, horizontaly);
        hctx.restore();
        vctx.restore();

        // clear the unusued nibs
        if (!showUpScrollNib) this.nibup = new Rect();
        if (!showDownScrollNib) this.nibdown = new Rect();
        if (!showLeftScrollNib) this.nibleft = new Rect();
        if (!showRightScrollNib) this.nibright = new Rect();
    },

    paintScrollbar: function(ctx, scrollbar) {
        var bar = scrollbar.getHandleBounds();
        var alpha = (ctx.globalAlpha) ? ctx.globalAlpha : 1;

        if (!scrollbar.isH()) {
            ctx.save();
            ctx.translate(bar.x + Math.floor(bar.w / 2), bar.y + Math.floor(bar.h / 2));
            ctx.rotate(Math.PI * 1.5);
            ctx.translate(-(bar.x + Math.floor(bar.w / 2)), -(bar.y + Math.floor(bar.h / 2)));

            // if we're vertical, the bar needs to be re-worked a bit
            bar = new Bespin.Editor.Rect(bar.x - Math.floor(bar.h / 2) + Math.floor(bar.w / 2),
                    bar.y + Math.floor(bar.h / 2) - Math.floor(bar.w / 2), bar.h, bar.w);
        }

        var halfheight = bar.h / 2;

        ctx.beginPath();
        ctx.arc(bar.x + halfheight, bar.y + halfheight, halfheight, Math.PI / 2, 3 * (Math.PI / 2), false);
        ctx.arc(bar.x2 - halfheight, bar.y + halfheight, halfheight, 3 * (Math.PI / 2), Math.PI / 2, false);
        ctx.lineTo(bar.x + halfheight, bar.y + bar.h);
        ctx.closePath();

        var gradient = ctx.createLinearGradient(bar.x, bar.y, bar.x, bar.y + bar.h);
        gradient.addColorStop(0, this.editor.theme.scrollBarFillGradientTopStart.sub("%a", alpha));
        gradient.addColorStop(0.4, this.editor.theme.scrollBarFillGradientTopStop.sub("%a", alpha));
        gradient.addColorStop(0.41, this.editor.theme.scrollBarFillStyle.sub("%a", alpha));
        gradient.addColorStop(0.8, this.editor.theme.scrollBarFillGradientBottomStart.sub("%a", alpha));
        gradient.addColorStop(1, this.editor.theme.scrollBarFillGradientBottomStop.sub("%a", alpha));
        ctx.fillStyle = gradient;
        ctx.fill();

        ctx.save();
        ctx.clip();

        ctx.fillStyle = this.editor.theme.scrollBarFillStyle.sub("%a", alpha);
        ctx.beginPath();
        ctx.moveTo(bar.x + (halfheight * 0.4), bar.y + (halfheight * 0.6));
        ctx.lineTo(bar.x + (halfheight * 0.9), bar.y + (bar.h * 0.4));
        ctx.lineTo(bar.x, bar.y + (bar.h * 0.4));
        ctx.closePath();
        ctx.fill();
        ctx.beginPath();
        ctx.moveTo(bar.x + bar.w - (halfheight * 0.4), bar.y + (halfheight * 0.6));
        ctx.lineTo(bar.x + bar.w - (halfheight * 0.9), bar.y + (bar.h * 0.4));
        ctx.lineTo(bar.x + bar.w, bar.y + (bar.h * 0.4));
        ctx.closePath();
        ctx.fill();

        ctx.restore();

        ctx.beginPath();
        ctx.arc(bar.x + halfheight, bar.y + halfheight, halfheight, Math.PI / 2, 3 * (Math.PI / 2), false);
        ctx.arc(bar.x2 - halfheight, bar.y + halfheight, halfheight, 3 * (Math.PI / 2), Math.PI / 2, false);
        ctx.lineTo(bar.x + halfheight, bar.y + bar.h);
        ctx.closePath();

        ctx.strokeStyle = this.editor.theme.scrollTrackStrokeStyle;
        ctx.stroke();

        if (!scrollbar.isH()) {
            ctx.restore();
        }
    },

    paintNib: function(ctx, nibStyle, arrowStyle, strokeStyle) {
        var midpoint = Math.floor(this.NIB_WIDTH / 2);

        ctx.fillStyle = nibStyle;
        ctx.beginPath();
        ctx.arc(0, 0, Math.floor(this.NIB_WIDTH / 2), 0, Math.PI * 2, true);
        ctx.closePath();
        ctx.fill();
        ctx.strokeStyle = strokeStyle;
        ctx.stroke();

        ctx.fillStyle = arrowStyle;
        ctx.beginPath();
        ctx.moveTo(0, -midpoint + this.NIB_ARROW_INSETS.top);
        ctx.lineTo(-midpoint + this.NIB_ARROW_INSETS.left, midpoint - this.NIB_ARROW_INSETS.bottom);
        ctx.lineTo(midpoint - this.NIB_ARROW_INSETS.right, midpoint - this.NIB_ARROW_INSETS.bottom);
        ctx.closePath();
        ctx.fill();
    }
});

// ** {{{ Bespin.Editor.API }}} **
//
// The root object. This is the API that others should be able to use

Bespin.Editor.API = Class.create({
    initialize: function(container) {
        this.container = container;
        this.model = new Bespin.Editor.DocumentModel();

        $(container).innerHTML = "<canvas id='canvas' moz-opaque='true' tabindex='-1'></canvas>";
        this.canvas = $(container).childElements()[0];

        this.ui = new Bespin.Editor.UI(this);
        this.theme = Bespin.Themes['default']; // set the default which is in themes.js (Coffee)
        this.cursorPosition = { row: 0, col: 0 }
        this.editorKeyListener = new Bespin.Editor.DefaultEditorKeyListener(this);
        this.undoManager = new Bespin.Editor.UndoManager(this);
        this.customEvents = new Bespin.Editor.Events(this);

        this.ui.installKeyListener(this.editorKeyListener);

        this.model.insertCharacters({row: 0, col: 0}, " ");

        Event.observe(this.canvas, "blur",  function(e) { this.setFocus(false); }.bindAsEventListener(this));
        Event.observe(this.canvas, "focus", function(e) { this.setFocus(true);  }.bindAsEventListener(this));

        this.paint();
    },

    moveCursor: function(newpos) {
        var row = Math.min(newpos.row, this.model.getRowCount() - 1); // last row if you go over
        if (row < 0) row = 0; // can't move negative off screen
        this.cursorPosition = { row: row, col: newpos.col };
    },

    // ensures that the start position is before the end position; reading directly from the selection property makes no such guarantee
    getSelection: function() {
        if (!this.selection) return undefined;

        var startPos = this.selection.startPos;
        var endPos = this.selection.endPos;

        // ensure that the start position is always before than the end position
        if ((endPos.row < startPos.row) || ((endPos.row == startPos.row) && (endPos.col < startPos.col))) {
            var foo = startPos;
            startPos = endPos;
            endPos = foo;
        }

        return { startPos: Bespin.Editor.Utils.copyPos(startPos), endPos: Bespin.Editor.Utils.copyPos(endPos) }
    },
    
    // helper to get text
    getSelectionAsText: function() {
        var selectionText = '';
        var selectionObject = this.getSelection();
        if (selectionObject) {
            selectionText = this.model.getChunk(selectionObject);
        }
        return selectionText;
    },

    setSelection: function(selection) {
        this.selection = selection;
        if (this.undoManager.syncHelper) this.undoManager.syncHelper.queueSelect(selection);
    },

    paint: function(fullRefresh) {
        var ctx = Bespin.Canvas.Fix(this.canvas.getContext("2d"));
        this.ui.paint(ctx, fullRefresh);
    },

    changeKeyListener: function(newKeyListener) {
        this.ui.installKeyListener(newKeyListener);
        this.editorKeyListener = newKeyListener;
    },

    // this does not set focus to the editor; it indicates that focus has been set to the underlying canvas
    setFocus: function(focus) {
        this.focus = focus;
    }
});

// ** {{{ Bespin.Editor.Events }}} **
//
// Handle custom events aimed at, and for the editor
Bespin.Editor.Events = Class.create({
    initialize: function(editor) {
        this.editor = editor;

        document.observe("bespin:editor:openfile:opensuccess", function(event) {
            var file = event.memo.file;

            editor.model.insertDocument(file.content);
            editor.moveCursor({ row: 0, col: 0 });
        });

        // -- fire an event here and you can run any editor action
        document.observe("bespin:editor:doaction", function(event) {
            var action = event.memo.action;
            var args   = event.memo.args || Bespin.Editor.Utils.buildArgs();

            if (action) editor.ui.actions[action](args);
        });

        // -- fire an event to setup any new or replace actions
        document.observe("bespin:editor:setaction", function(event) {
            var action = event.memo.action;
            var code   = event.memo.code;
            if (action && Object.isFunction(code)) editor.ui.actions[action] = code;
        });

        // -- add key listeners
        // e.g. bindkey ctrl b moveCursorLeft
        document.observe("bespin:editor:bindkey", function(event) {
            var modifiers = event.memo.modifiers || '';
            if (!event.memo.key) return;

            var keyCode = Bespin.Key[event.memo.key.toUpperCase()];

            // -- try an editor action first, else fire away at the event bus
            var action = editor.ui.actions[event.memo.action] || event.memo.action;

            if (keyCode && action) {
                editor.editorKeyListener.bindKeyString(modifiers, keyCode, action);
            }
        });

    }
});
