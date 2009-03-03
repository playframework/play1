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

// = Bespin =
//
// This is the root of it all. The {{{Bespin}}} namespace.
// All of the JavaScript for Bespin will be placed in this namespace later.
//
// {{{ Bespin.versionNumber }}} is the core version of the Bespin system
// {{{ Bespin.apiVersion }}} is the version number of the API (to ensure that the
//                          client and server are talking the same language)
// {{{ Bespin.commandlineHeight }}} is the height of the command line

var Bespin = {
    // BEGIN VERSION BLOCK
    versionNumber: 'tip',
    versionCodename: '(none)',
    apiVersion: 'dev',
    // END VERSION BLOCK

    commandlineHeight: 95,
    userSettingsProject: "BespinSettings",
   
    displayVersion: function(el) {
        if (!el) el = $("version");
        if (!el) return;
        el.innerHTML = '<a href="https://wiki.mozilla.org/Labs/Bespin/ReleaseNotes" title="Read the release notes">Version <span class="versionnumber">' + this.versionNumber + '</span> "' + this.versionCodename + '"</a>';
    }
};
