// = Element.enableTextSelection(element, isEnabled) =
//
// Enables or disables text selection within the given element. 
// Straps onto Prototype Elements
//
// * element (Element): The element within which a dragging motion
// * _should_ or _should not_ behave like text selection.
// * isEnabled (boolean): Whether to enable or disable text selection.
// 
// NOTE: Based on http://andrewdupont.net/2009/01/05/code-disabling-text-selection/

(function() {
  var IGNORED_ELEMENTS = [];

  function _textSelectionHandler(event) {
    var element = Event.element(event);
        
    if (!element) return;
    
    for (var i = 0, node; node = IGNORED_ELEMENTS[i]; i++) {
      if (element === node || element.descendantOf(node)) {

        if (typeof element.style.MozUserSelect != "undefined") { // Firefox++
        	element.style.MozUserSelect = "none";
        }
        element.style.cursor = "default"; // turn off the selection cursor
          
        Event.stop(event);
        break;
      }
    }
  }
  
  if (document.attachEvent)
    document.onselectstart = _textSelectionHandler.bindAsEventListener(window);    
  else
    document.observe('mousedown', _textSelectionHandler);

  // ** {{{ Element.addMethods }}} **
  //
  // Add the enableTextSelection hook onto Element
  Element.addMethods({
    enableTextSelection: function(element, isEnabled) {
      if (isEnabled) {
        IGNORED_ELEMENTS = IGNORED_ELEMENTS.without(element);
      } else {
        if (!IGNORED_ELEMENTS.include(element))
          IGNORED_ELEMENTS.push(element);
      }
    }
  });
})();