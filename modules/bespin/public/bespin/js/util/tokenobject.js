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

// = Bespin.TokenObject =
//
// Given a string, make a token object that holds positions and has name access
//
// Examples,
//
// * args = new Bespin.TokenObject(userString, { params: command.takes.order.join(' ') });
// * var test = new Bespin.TokenObject(document.getElementById("input").value, { 
//	     splitBy: document.getElementById("regex").value,
//	     params: document.getElementById("params").value
// });

if (typeof Bespin == "undefined") Bespin = {};

Bespin.TokenObject = function(input, options) {
    this._input = input;
    this._options = options;
    this._splitterRegex = new RegExp(this._options.splitBy || '\\s+');
    this._pieces = input.split(this._splitterRegex);

    if (this._options.params) { // -- create a hash for name based access
        this._nametoindex = {};
        var namedparams = this._options.params.split(' ');
        for (var x = 0; x < namedparams.length; x++) {
            this._nametoindex[namedparams[x]] = x;

            if (!this._options['noshortcutvalues']) { // side step if you really don't want this
                this[namedparams[x]] = this._pieces[x];
            }
        }

    }
}

Bespin.TokenObject.prototype.param = function(index) {
    return (typeof index == "number") ? this._pieces[index] : this._pieces[this._nametoindex[index]];
}

Bespin.TokenObject.prototype.length = function() {
    return this._pieces.length;
}
