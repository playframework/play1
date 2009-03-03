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

// = Toolbar =
//
// The editor has the notion of a toolbar which are components that can drive the editor from outside of itself
// Such examples are collaboration views, file browser, undo/redo, cut/copy/paste and more.

Bespin.Editor.Toolbar = Class.create({
    DEFAULT_TOOLBAR: ["collaboration", "files", "dashboard", "target_browsers", "save",
                      "close", "undo", "redo", "cut", "copy", "paste", "preview", "fontsize"],
    FONT_SIZES: {
        1: 8,  // small
        2: 10, // medium
        3: 14  // large
    },

    initialize: function(editor) {
        this.editor = editor || _editor;
        this.currentFontSize = 2;
    },
    
    setup: function(type, el) {
        if (Object.isFunction(this.components[type])) this.components[type](this, el);
    },

    /*
     * Go through the default list and try to hitch onto the DOM element
     */
    setupDefault: function() {
        this.DEFAULT_TOOLBAR.each(function(item) {
            var item_el = $("toolbar_" + item);
            if (item_el) {
                this.setup(item, item_el);
            }
        }.bind(this));
    },
    
    components: {
        collaboration: function(toolbar, el) {
            var collab = $(el) || $("toolbar_collaboration");
            Element.observe(collab, 'click', function() {
                _showCollab = !_showCollab;
                collab.src = "images/" + ( (_showCollab) ? "icn_collab_on.png" : (_showCollabHotCounter == 0) ? "icn_collab_off.png" : "icn_collab_watching.png" );
                if (Object.isFunction(recalcLayout)) recalcLayout(); // todo fix
            });
            Element.observe(collab, 'mouseover', function() {
                collab.style.cursor = "pointer";
                collab.src = "images/icn_collab_on.png";
            });
            Element.observe(collab, 'mouseout', function() {
                collab.style.cursor = "default";
                collab.src = "images/icn_collab_off.png";
            });
        },
        
        files: function(toolbar, el) {
            var files = $(el) || $("toolbar_files");
            Element.observe(files, 'click', function() {
                _showFiles = !_showFiles;
                files.src = "images/" + ( (_showFiles) ? "icn_files_on.png" : "icn_files_off.png" );
                if (Object.isFunction(recalcLayout)) recalcLayout(); // todo fix
            });
            Element.observe(files, 'mouseover', function() {
                files.style.cursor = "pointer";
                files.src = "images/icn_files_on.png";
            });
            Element.observe(files, 'mouseout', function() {
                files.style.cursor = "default";
                files.src = "images/icn_files_off.png";
            });
        },

        dashboard: function(toolbar, el) {
            var dashboard = $(el) || $("toolbar_dashboard");
            Element.observe(dashboard, 'mouseover', function() {
                dashboard.style.cursor = "pointer";
                dashboard.src = "images/icn_dashboard_on.png";
            });
            Element.observe(dashboard, 'mouseout', function() {
                dashboard.style.cursor = "default";
                dashboard.src = "images/icn_dashboard_off.png";
            });
        },
        
        target_browsers: function(toolbar, el) {
            var target = $(el) || $("toolbar_target_browsers");
            Element.observe(target, 'click', function() {
                _showTarget = !_showTarget;
                target.src = "images/" + ( (_showTarget) ? "icn_target_on.png" : "icn_target_off.png" );
                if (Object.isFunction(recalcLayout)) recalcLayout(); // todo fix
            });
            Element.observe(target, 'mouseover', function() {
                target.style.cursor = "pointer";
                target.src = "images/icn_target_on.png";
            });
            Element.observe(target, 'mouseout', function() {
                target.style.cursor = "default";
                target.src = "images/icn_target_off.png";
            });
        },

        save: function(toolbar, el) {
            var save = $(el) || $("toolbar_save");
            Element.observe(save, 'mousedown', function() {
                save.src = "images/icn_save_on.png";
            });

            Element.observe(save, 'mouseup', function() {
                save.src = "images/icn_save.png";
            });

            Element.observe(save, 'click', function() {
                document.fire("bespin:editor:savefile");
            });
        },

        close: function(toolbar, el) {
            var close = $(el) || $("toolbar_close");
            Element.observe(close, 'mousedown', function() {
                close.src = "images/icn_close_on.png";
            });

            Element.observe(close, 'mouseup', function() {
                close.src = "images/icn_close.png";
            });

            Element.observe(close, 'click', function() {
                document.fire("bespin:editor:closefile");
            });
        },

        undo: function(toolbar, el) {
            var undo = $(el) || $("toolbar_undo");
            Element.observe(undo, 'mousedown', function() {
                undo.src = "images/icn_undo_on.png";
            });

            Element.observe(undo, 'mouseup', function() {
                undo.src = "images/icn_undo.png";
            });

            Element.observe(undo, 'click', function() {
                toolbar.editor.ui.actions.undo();
            });
        },

        redo: function(toolbar, el) {
            var redo = $(el) || $("toolbar_undo");

            Element.observe(redo, 'mousedown', function() {
                redo.src = "images/icn_redo_on.png";
            });

            Element.observe(redo, 'mouseup', function() {
                redo.src = "images/icn_redo.png";
            });

            Element.observe(redo, 'click', function() {
                toolbar.editor.ui.actions.redo();
            });
        },
        
        cut: function(toolbar, el) {
            var cut = $(el) || $("toolbar_cut");

            Element.observe(cut, 'mousedown', function() {
                cut.src = "images/icn_cut_on.png";
            });

            Element.observe(cut, 'mouseup', function() {
                cut.src = "images/icn_cut.png";
            });

            Element.observe(cut, 'click', function() {
                toolbar.editor.ui.actions.cutSelection(Bespin.Editor.Utils.buildArgs());
            });
        },

        copy: function(toolbar, el) {
            var copy = $(el) || $("toolbar_copy");

            Element.observe(copy, 'mousedown', function() {
                copy.src = "images/icn_copy_on.png";
            });

            Element.observe(copy, 'mouseup', function() {
                copy.src = "images/icn_copy.png";
            });

            Element.observe(copy, 'click', function() {
                toolbar.editor.ui.actions.copySelection(Bespin.Editor.Utils.buildArgs());
            });
        },

        paste: function(toolbar, el) {
            var paste = $(el) || $("toolbar_paste");

            Element.observe(paste, 'mousedown', function() {
                paste.src = "images/icn_paste_on.png";
            });

            Element.observe(paste, 'mouseup', function() {
                paste.src = "images/icn_paste.png";
            });

            Element.observe(paste, 'click', function() {
                toolbar.editor.ui.actions.pasteFromClipboard(Bespin.Editor.Utils.buildArgs());
            });
        },

        // history: function(toolbar, el) {
        //     var history = $(el) || $("toolbar_history");
        //     
        //     Element.observe(history, 'mousedown', function() {
        //         history.src = "images/icn_history_on.png";
        //     });
        // 
        //     Element.observe(history, 'mouseup', function() {
        //         history.src = "images/icn_history.png";
        //     });
        //     
        //     Element.observe(history, 'click', function() {
        //         console.log("clicked on history toolbar icon");
        //     });
        // },

        preview: function(toolbar, el) {
            var preview = $(el) || $("toolbar_preview");
            
            Element.observe(preview, 'mousedown', function() {
                preview.src = "images/icn_preview_on.png";
            });

            Element.observe(preview, 'mouseup', function() {
                preview.src = "images/icn_preview.png";
            });
            
            Element.observe(preview, 'click', function() {
                document.fire("bespin:editor:preview"); // use default file                
            });
        },

        fontsize: function(toolbar, el) {
            var fontsize = $(el) || $("toolbar_fontsize");
            
            Element.observe(fontsize, 'mousedown', function() {
                fontsize.src = "images/icn_fontsize_on.png";
            });

            Element.observe(fontsize, 'mouseup', function() {
                fontsize.src = "images/icn_fontsize.png";
            });

            // Change the font size between the small, medium, and large settings
            Element.observe(fontsize, 'click', function() {
                toolbar.currentFontSize = (toolbar.currentFontSize > 2) ? 1 : toolbar.currentFontSize + 1;
                document.fire("bespin:settings:set:fontsize", { value: toolbar.FONT_SIZES[toolbar.currentFontSize] });
            });
        }
    }
});