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

// = Clipboard =
//
// Handle clipboard operations. 
// If using WebKit (I know, feature detection would be nicer, but e.clipboardData is deep) use DOMEvents
// Else try the bad tricks.

// ** {{{ Bespin.Clipboard }}} **
//
// The factory that is used to install, and setup the adapter that does the work

Bespin.Clipboard = {
    // ** {{{ install }}} **
    //
    // Given a clipboard adapter implementation, save it, an call install() on it
    install: function(newImpl) {
        if (this.uses && typeof this.uses['uninstall'] == "function") this.uses.uninstall();
        this.uses = newImpl;
        this.uses.install();
    },

    // ** {{{ setup }}} **
    //
    // Do the first setup. Right now checks for WebKit and inits a DOMEvents solution if that is true
    // else install the default.
    setup: function() {
        if (Prototype.Browser.WebKit) {
            this.install(new Bespin.Clipboard.DOMEvents());
        } else {
            this.install(new Bespin.Clipboard.Default());
        }
    }
};

// ** {{{ Bespin.Clipboard.DOMEvents }}} **
//
// This adapter configures the DOMEvents that only WebKit seems to do well right now.
// There is trickery involved here. The before event changes focus to the hidden
// copynpaster text input, and then the real event does its thing and we focus back

Bespin.Clipboard.DOMEvents = Class.create({
    install: function() {
        
        // * Configure the hidden copynpaster element
        var copynpaster = document.createElement("input");
        Element.writeAttribute(copynpaster, {
            type: 'text',
            id: 'copynpaster',
            style: "position: absolute; z-index: -400; top: -100px; left: -100px; width: 0; height: 0; border: none;"
        });
        document.body.appendChild(copynpaster);
        
        // Copy
        Event.observe(document, "beforecopy", function(e) {
            e.preventDefault();
            $('copynpaster').focus();
        });

        Event.observe(document, "copy", function(e) {
            var selectionText = _editor.getSelectionAsText();
            
            if (selectionText && selectionText != '') {
                e.preventDefault();
                e.clipboardData.setData('text/plain', selectionText);
            }
            
            $('canvas').focus();
        });

        // Cut
        Event.observe(document, "beforecut", function(e) {
            e.preventDefault();
            $('copynpaster').focus();
        });

        Event.observe(document, "cut", function(e) {
            var selectionObject = _editor.getSelection();

            if (selectionObject) {
                var selectionText = _editor.model.getChunk(selectionObject);

                if (selectionText && selectionText != '') {
                    e.preventDefault();
                    e.clipboardData.setData('text/plain', selectionText);
                    _editor.ui.actions.deleteSelection(selectionObject);
                }
            }

            $('canvas').focus();
        });

        // Paste
        Event.observe(document, "beforepaste", function(e) {
            e.preventDefault();
            $('copynpaster').focus();
        });

        Event.observe(document, "paste", function(e) {
            e.preventDefault();

            var args = Bespin.Editor.Utils.buildArgs();    
            args.chunk = e.clipboardData.getData('text/plain');
            if (args.chunk) _editor.ui.actions.insertChunk(args);

            $('canvas').focus();
            $('copynpaster').value = '';
        });

        Event.observe(document, "dom:loaded", function() {
            Event.observe($('copynpaster'), "keydown", function(e) {
                e.stopPropagation();
            });

            Event.observe($('copynpaster'), "keypress", function(e) {
                e.stopPropagation();
            });
        });        
    },
    
    uninstall: function() {
        Event.stopObserving($('copynpaster'), "keypress");
        Event.stopObserving($('copynpaster'), "keydown");
        Event.stopObserving(document, "beforepaste");
        Event.stopObserving(document, "paste");
        Event.stopObserving(document, "beforecut");
        Event.stopObserving(document, "cut");
        Event.stopObserving(document, "beforecopy");
        Event.stopObserving(document, "copy");
    }
});

// ** {{{ Bespin.Clipboard.Default }}} **
//
// Turn on the key combinations to access the Bespin.Clipboard.Manual class 

Bespin.Clipboard.Default = Class.create({
    install: function() {
        var copyArgs = Bespin.Key.fillArguments("APPLE C");
        copyArgs.action = "copySelection";
        document.fire("bespin:editor:bindkey", copyArgs);
        copyArgs = Bespin.Key.fillArguments("CTRL C");
        document.fire("bespin:editor:bindkey", copyArgs);

        var pasteArgs = Bespin.Key.fillArguments("APPLE V");
        pasteArgs.action = "pasteFromClipboard";
        document.fire("bespin:editor:bindkey", pasteArgs);
        pasteArgs = Bespin.Key.fillArguments("CTRL V");
        document.fire("bespin:editor:bindkey", pasteArgs);

        var cutArgs = Bespin.Key.fillArguments("APPLE X");
        cutArgs.action = "cutSelection";
        document.fire("bespin:editor:bindkey", cutArgs);
        cutArgs = Bespin.Key.fillArguments("CTRL X");
        document.fire("bespin:editor:bindkey", cutArgs);
    }
});

// ** {{{ Bespin.Clipboard.Manual }}} **
//
// The ugly hack that tries to use XUL to get work done, but will probably fall through to in-app copy/paste only
Bespin.Clipboard.Manual = new function() {
    var clipdata;
    
    return {
        copy: function(copytext) {
            try {
                if (netscape.security.PrivilegeManager.enablePrivilege) {
                    netscape.security.PrivilegeManager.enablePrivilege("UniversalXPConnect");
                } else {
                    clipdata = copytext;
                    return;
                }
            } catch (ex) {
                clipdata = copytext;
                return;
            }

            var str = Components.classes["@mozilla.org/supports-string;1"].
                                      createInstance(Components.interfaces.nsISupportsString);
            str.data = copytext;

            var trans = Components.classes["@mozilla.org/widget/transferable;1"].
                                   createInstance(Components.interfaces.nsITransferable);
            if (!trans) return false;

            trans.addDataFlavor("text/unicode");
            trans.setTransferData("text/unicode", str, copytext.length * 2);

            var clipid = Components.interfaces.nsIClipboard;
            var clip   = Components.classes["@mozilla.org/widget/clipboard;1"].getService(clipid);
            if (!clip) return false;

            clip.setData(trans, null, clipid.kGlobalClipboard);

            /*
            // Flash doesn't work anymore :(
            if (inElement.createTextRange) {
                var range = inElement.createTextRange();
                if (range && BodyLoaded==1)
                    range.execCommand('Copy');
            } else {
                var flashcopier = 'flashcopier';
                if(!document.getElementById(flashcopier)) {
                    var divholder = document.createElement('div');
                    divholder.id = flashcopier;
                    document.body.appendChild(divholder);
                }
                document.getElementById(flashcopier).innerHTML = '';

                var divinfo = '<embed src="_clipboard.swf" FlashVars="clipboard='+escape(inElement.value)+'" width="0" height="0" type="application/x-shockwave-flash"></embed>';
                document.getElementById(flashcopier).innerHTML = divinfo;
            }
            */
        },

        data: function() {
            try {
                if (netscape.security.PrivilegeManager.enablePrivilege) {
                    netscape.security.PrivilegeManager.enablePrivilege("UniversalXPConnect");
                } else {
                    return clipdata;
                }
            } catch (ex) {
                return clipdata;
            }

            var clip = Components.classes["@mozilla.org/widget/clipboard;1"].getService(Components.interfaces.nsIClipboard);
            if (!clip) return false;

            var trans = Components.classes["@mozilla.org/widget/transferable;1"].createInstance(Components.interfaces.nsITransferable);
            if (!trans) return false;
            trans.addDataFlavor("text/unicode");

            clip.getData(trans, clip.kGlobalClipboard);

            var str       = new Object();
            var strLength = new Object();
            var pastetext = "";

            trans.getTransferData("text/unicode", str, strLength);
            if (str) str = str.value.QueryInterface(Components.interfaces.nsISupportsString);
            if (str) pastetext = str.data.substring(0, strLength.value / 2);
            return pastetext;
        }
    }
}();

// ** {{{ Event: dom:loaded }}} **
//
// Call into setup to get working.
Event.observe(document, "dom:loaded", function() {
    Bespin.Clipboard.setup(); // Do it!
});

