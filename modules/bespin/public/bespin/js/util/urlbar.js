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

// = URLBar =
//
// URLBar watches the browser URL navigation bar for changes. 
// If it sees a change it tries to open the file
// The common case is using the back/forward buttons

Bespin.URLBar = {
    last: document.location.hash,
    check: function() {
        var hash = document.location.hash;
        if (this.last != hash) {
            var urlchange = new Bespin.Settings.URL(hash);
            document.fire("bespin:editor:openfile", { filename: urlchange.get('path') });
            this.last = hash;
        }
    }
};

setInterval(function() {
    Bespin.URLBar.check.apply(Bespin.URLBar);
}, 200);
