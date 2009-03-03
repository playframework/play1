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

// = Syntax Highlighting =
//
// Module for dealing with the syntax highlighting.
//
// The core model talks to specific engines to do the work and then packages it up to send to the editor. 

if (typeof Bespin == "undefined") Bespin = {};
if (!Bespin.Syntax) Bespin.Syntax = {};


// ** {{{ Bespin.Syntax.Model }}} **
//
// Tracks syntax highlighting data on a per-line basis.

Bespin.Syntax.Model = Class.create({
    initialize: function(editor) {
        this.editor = editor;
        this.lineCache = [];
        this.lineMetaInfo = [];
        this.syntaxType = "";
    },
    
    // ** {{{ Meta Info }}} **
    //
    // We store meta info on the lines, such as the fact that it is in a multiline comment
    setLineMetaInfo: function(lineNumber, meta) {
        this.lineMetaInfo[lineNumber] = meta;
    },
    
    getLineMetaInfo: function(lineNumber) {
        return this.lineMetaInfo[lineNumber];
    },

    // ** {{{ Caching }}} **
    //
    // Optionally, keep a cache of the highlighted model
    invalidateCache: function(lineNumber) {
        delete this.lineCache[lineNumber];
    },

    invalidateEntireCache: function() {
        this.lineCache = [];
    },
    
    addToCache: function(lineNumber, line) {
        this.lineCache[lineNumber] = line;
    },
    
    getFromCache: function(lineNumber) {
        return this.lineCache[lineNumber];
    },
    
    mergeSyntaxResults: function(regions) {
        // TO BE COMPLETED
        // This function has to take the regions and take sub pieces and tie them into the full line
        // For example, imagine an HTML engine that sees <script>....</script>
        // It will pass .... into the JavaScript engine and take those results with a base of 0 and return the real location
        var base = 0;
        for (var i = 0; i < regions.length; i++) {
            var region = region[i];
            //base += region.
        }
    },

    // -- Main API
    // ** {{{ getSyntaxStyles }}} **
    //
    // This is the main API.
    //
    // Given the line number and syntax type (e.g. css, js, html) hunt down the engine, ask it to syntax highlight, and return the regions
    getSyntaxStyles: function(lineNumber, syntaxType) {
        if (this.syntaxType != syntaxType) {
            this.invalidateEntireCache();
            this.engine = Bespin.Syntax.EngineResolver.resolve(syntaxType);
            this.syntaxType = syntaxType;
        } else { // Possible for caching to be real here
            // var cached = this.getFromCache(lineNumber);
            // if (cached) return cached;            
        }

        // Get the row contents as one string
        var line = this.editor.model.getRowArray(lineNumber).join("");

        var syntaxResult = { // setup the result
            text: line,
            regions: []
        };
        
        // we have the ability to have subtypes within the main parser
        // E.g. HTML can have JavaScript or CSS within
        if (typeof this.engine['innertypes'] == "function") {
            var syntaxTypes = this.engine.innertypes(line);

            for (var i = 0; i < syntaxTypes.length; i++) {
                var type = syntaxTypes[i];
                var meta = { inMultiLineComment: this.inMultiLineComment(), offset: type.start }; // pass in an offset
                var pieceRegions = [];
                var fromResolver = Bespin.Syntax.EngineResolver.highlight(type.type, line.substring(type.start, type.stop), meta);
                if (fromResolver.meta && (i == syntaxTypes.length - 1) ){
                    this.setLineMetaInfo(lineNumber, fromResolver.meta);
                }
                pieceRegions.push(fromResolver);
            }
            syntaxResult.regions.push(this.mergeSyntaxResults(pieceRegions));
        } else {
            var meta = (lineNumber > 0) ? this.getLineMetaInfo(lineNumber - 1) : {};
            var result = this.engine.highlight(line, meta);
            this.setLineMetaInfo(lineNumber, result.meta);
            syntaxResult.regions.push(result.regions);
        }
        
        this.addToCache(lineNumber, syntaxResult);
        return syntaxResult;
    }
});

// ** {{{ Bespin.Syntax.EngineResolver }}} **
//
// The resolver holds the engines that are available to do the actual syntax highlighting
//
// It holds a default engine that returns the line back in the clear

Bespin.Syntax.EngineResolver = new function() {
  var engines = {};
  
  // ** {{{ NoopSyntaxEngine }}} **
  //
  // Return a plain region that is the entire line
  var NoopSyntaxEngine = {
      highlight: function(line, meta) {
          return { regions: {
              plain: [{
                  start: 0,
                  stop: line.length
              }]
          } }
      }
      //innersyntax: function() {},
  }
  
  return {
      // ** {{{ highlight }}} **
      //
      // A high level highlight function that uses the {{{type}}} to get the engine, and asks it to highlight
      highlight: function(type, line, meta) {
          this.resolve(type).highlight(line, meta);
      },

      // ** {{{ register }}} **
      //
      // Engines register themselves, 
      // e.g. {{{Bespin.Syntax.EngineResolver.register(new Bespin.Syntax.CSSSyntaxEngine(), ['css']);}}}
      register: function(syntaxEngine, types) {
          for (var i = 0; i < types.length; i++) {
              engines[types[i]] = syntaxEngine;
          }
      },

      // ** {{{ resolve }}} **
      //
      // Hunt down the engine for the given {{{type}}} (e.g. css, js, html) or return the {{{NoopSyntaxEngine}}}
      resolve: function(type) {
          return engines[type] || NoopSyntaxEngine;
      }      
  }  
}();
