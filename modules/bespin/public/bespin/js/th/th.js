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

// install a console if not present
if (!console) var console = { log: function(textToLog) {} };

// force back an array
Array.asArray = function(object) {
    return Object.isArray(object) ? object : [ object ];
};

/*
    Constants
 */
var TH = {
    VERTICAL: "v",
    HORIZONTAL: "h"
}

/*
    Event bus; all listeners and events pass through a single global instance of this class.
 */
var Bus = Class.define({
    members: {
        init: function() {
            // map of event name to listener; listener contains a selector, function, and optional context object
            this.events = {};
        },

        // register a listener with an event
        bind: function(event, selector, listenerFn, listenerContext) {
            var listeners = this.events[event];
            if (!listeners) {
                listeners = [];
                this.events[event] = listeners;
            }
            selector = Array.asArray(selector);
            for (var z = 0; z < selector.length; z++) {
                for (var i = 0; i < listeners.length; i++) {
                    if (listeners[i].selector == selector[z] && listeners[i].listenerFn == listenerFn) return;
                }
                listeners.push({ selector: selector[z], listenerFn: listenerFn, context: listenerContext });
            }
        },

        // removes any listeners whose selectors have the *same identity* as the passed selector
        unbind: function(selector) {
            for (var event in this.events) {
                var listeners = this.events[event];

                for (var i = 0; i < listeners.length; i++) {
                    if (listeners[i].selector === selector) {
                        this.events[event] = listeners.without(listeners[i]);
                        listeners = this.events[event];
                        i--;
                    }
                }
            }
        },

        // notify all listeners of an event
        fire: function(eventName, eventDetails, component) {
            var listeners = this.events[eventName];
            if (!listeners || listeners.length == 0) return;

            // go through each listener registered for the fired event and check if the selector matches the component for whom
            // the event was fired; if there is a match, dispatch the event
            for (var i = 0; i < listeners.length; i++) {
                // if the listener selector is a string...
                if (listeners[i].selector.constructor == String) {
                    // check if the string starts with a hash, indicating that it should match by id
                    if (listeners[i].selector.charAt(0) == "#") {
                        if (component.id == listeners[i].selector.substring(1)) {
                            this.dispatchEvent(eventName, eventDetails, component, listeners[i]);
                        }
                    // otherwise check if it's the name of the component class
                    } else if (listeners[i].selector == component.type) {
                        this.dispatchEvent(eventName, eventDetails, component, listeners[i]);
                    }
                // otherwise check if the selector is the current component
                } else if (listeners[i].selector == component) {
                    this.dispatchEvent(eventName, eventDetails, component, listeners[i]);
                }
            }
        },

        // invokes the listener function
        dispatchEvent: function(eventName, eventDetails, component, listener) {
            eventDetails.thComponent = component;

            // check if there is listener context; if so, execute the listener function using that as the context
            if (listener.context) {
                listener.listenerFn.apply(listener.context, [ eventDetails ]);
            } else {
                listener.listenerFn(eventDetails);
            }
        }
    }
});

// create the global event bus
var th_global_event_bus = new Bus();

var Scene = Class.define({
    uses: [
        EventHelpers
    ],

    members: {
        bus: th_global_event_bus,

        css: {},

        sheetCount: 0,
        currentSheet: 0,
        cssLoaded: false,
        renderRequested: false,

        init: function(canvas) {
            this.canvas = canvas;

            var self = this;
            Event.observe(window, "resize", function() {
                self.render();
            });

            this.root = new Panel({ id: "root", style: {
//                backgroundColor: "pink" // for debugging         
            } });
            this.root.scene = this;

            var testCanvas = document.createElement("canvas");
            this.scratchContext = testCanvas.getContext("2d");
            Bespin.Canvas.Fix(this.scratchContext);

            var self = this;

            Event.observe(window, "mousedown", function(e) {
                self.wrapEvent(e, self.root);

                self.mouseDownComponent = e.thComponent;

                th_global_event_bus.fire("mousedown", e, e.thComponent);
            });

            Event.observe(window, "dblclick", function(e) {
                self.wrapEvent(e, self.root);

                th_global_event_bus.fire("dblclick", e, e.thComponent);
            });

            Event.observe(window, "click", function(e) {
                self.wrapEvent(e, self.root);

                th_global_event_bus.fire("click", e, e.thComponent);
            });

            Event.observe(window, "mousemove", function(e) {
                self.wrapEvent(e, self.root);

                th_global_event_bus.fire("mousemove", e, e.thComponent);

                if (self.mouseDownComponent) {
                    self.addComponentXY(e, self.root, self.mouseDownComponent);
                    th_global_event_bus.fire("mousedrag", e, self.mouseDownComponent);
                }
            });

            Event.observe(window, "mouseup", function(e) {
                if (!self.mouseDownComponent) return;

                self.addComponentXY(e, self.root, self.mouseDownComponent);
                th_global_event_bus.fire("mouseup", e, self.mouseDownComponent);

                delete self.mouseDownComponent;
            });

            this.parseCSS();
        },

        render: function() {
            if (!this.cssLoaded) {
                this.renderRequested = true;
                return;
            }

            this.layout();
            this.paint();
        },

        layout: function() {
            if (this.root) {
                this.root.bounds = { x: 0, y: 0, width: this.canvas.width, height: this.canvas.height };
                this.root.layoutTree();
            }
        },

        paint: function(component) {
            if (!this.cssLoaded) {
                this.renderRequested = true;
                return;
            }

            if (!component) component = this.root;

            //if (component === this.root) console.log("root paint");

            if (component) {
                if (!component.opaque && component.parent) {
                    return this.paint(component.parent);
                }

                var ctx = this.canvas.getContext("2d");
                Bespin.Canvas.Fix(ctx);

                ctx.save();

                var parent = component.parent;
                var child = component;
                while (parent) {
                    ctx.translate(child.bounds.x, child.bounds.y);
                    child = parent;
                    parent = parent.parent;
                }

                ctx.clearRect(0, 0, component.bounds.width, component.bounds.height);
                ctx.beginPath();
                ctx.rect(0, 0, component.bounds.width, component.bounds.height);
                ctx.closePath();
                ctx.clip();
                component.paint(ctx);

                ctx.restore();
            }
        },

        parseCSS: function() {
            var self = this;
            var links = $$("link[rel='thstylesheet']");
            if (links.length == 0) {
                this.cssLoaded = true;
                return;
            }
            this.sheetCount = links.length;
            links.each(function(link) {
                new Ajax.Request(link.href, {
                    method: 'GET',
                    onSuccess: function(xhr) {
                        self.processCSS(xhr.responseText);
                    }
                });
            });
        },

        processCSS: function(stylesheet) {
            this.css = new CSSParser().parse(stylesheet, this.css);

            if (++this.currentSheet == this.sheetCount) {
                this.cssLoaded = true;
                if (this.renderRequested) {
                    this.render();
                    this.renderRequested = false;
                }
            }
        }
    }
});

var Border = Class.define({
    type: "Border",

    uses: [
        ComponentHelpers
    ],

    members: {
        // component
        init: function(parms) {
            if (!parms) parms = {};
            this.style = parms.style || {};
            this.attributes = parms.attributes || {};
        },

        getInsets: function() {
            return this.emptyInsets();
        },

        paint: function(ctx) {}
    }
});

var Component = Class.define({
    type: "Component",

    uses: [
        ComponentHelpers
    ],

    members: {
        init: function(parms) {
            if (!parms) parms = {};
            this.bounds = parms.bounds || {};
            this.style = parms.style || {};
            this.attributes = parms.attributes || {};
            this.id = parms.id;
            this.border = parms.border;
            this.opaque = parms.opaque || true;

            this.bus = th_global_event_bus;
        },

        // used to obtain a throw-away canvas context for performing measurements, etc.; may or may not be the same canvas as that used to draw the component
        getScratchContext: function() {
            var scene = this.getScene();
            if (scene) return scene.scratchContext;
        },

        getPreferredHeight: function(width) {},

        getPreferredWidth: function(height) {},

        getInsets: function() {
            return (this.border) ? this.border.getInsets() : this.emptyInsets();
        },

        paint: function(ctx) {
            //console.log("default component paint");
        },

        repaint: function() {
            this.getScene().paint(this);
        }
    }
});

var Container = Class.define({
    type: "Container",

    superclass: Component,

    uses: [
        ContainerHelpers
    ],

    members: {
        init: function(parms) {
            this._super(parms);
            this.children = [];
        },

        add: function() {
            for (var z = 0; z < arguments.length; z++) {
                component = Array.asArray(arguments[z]); // wrap as array if necessary
                this.children = this.children.concat(component);
                for (var i = 0; i < component.length; i++) {
                    component[i].parent = this;
                }
            }
        },

        remove: function() {
            for (var z = 0; z < arguments.length; z++) {
                component = Array.asArray(arguments[z]); // wrap as array if necessary
                for (var i = 0; i < component.length; i++) {
                    var old_length = this.children.length;
                    this.children = this.children.without(component[i]);

                    // if the length of the array has changed since I tried to remove the current component, assume it was removed and clear the parent
                    if (old_length != this.children.length) delete component[i].parent;
                }
            }
        },

        paint: function(ctx) {
            if (this.shouldPaint()) {
                this.paintSelf(ctx);
                this.paintChildren(ctx);
            }
        },

        paintSelf: function(ctx) {},

        paintChildren: function(ctx) {
            for (var i = 0; i < this.children.length; i++ ) {
                if (!this.children[i].shouldPaint()) continue;

                if (!this.children[i].bounds) {
                    //console.log("WARNING: child " + i + " (type: " + this.children[i].type + ", id: " + this.children[i].id + ") of parent with id " + this.id + " of type " + this.type + " has no bounds and could not be painted");
                    continue;
                }

                ctx.save();
                try {
                    ctx.translate(this.children[i].bounds.x, this.children[i].bounds.y);
                } catch (error) {
                    //console.log("WARNING: child " + i + " (type: " + this.children[i].type + ", id: " + this.children[i].id + ") of parent with id " + this.id + " of type " + this.type + " has malformed bounds and could not be painted");
                    //console.log(this.children[i].bounds);
                    ctx.restore();
                    continue;
                }

                try {
                    if (!this.children[i].style["noClip"]) {
                        ctx.beginPath();
                        ctx.rect(0, 0, this.children[i].bounds.width, this.children[i].bounds.height);
                        ctx.closePath();
                        ctx.clip();
                    }
                } catch(ex) {
                    // console.log("Bounds problem");
                    // console.log(this.children[i].type);
                    // console.log(this.children[i].bounds);
                }

                ctx.save();
                this.children[i].paint(ctx);
                ctx.restore();

                if (this.children[i].style.border) {
                    this.children[i].style.border.component = this.children[i];
                    ctx.save();
                    this.children[i].style.border.paint(ctx);
                    ctx.restore();
                }

                ctx.restore();
            }
        },

        // lays out this container and any sub-containers
        layoutTree: function() {
            this.layout();
            for (var i = 0; i < this.children.length; i++) {
                if (this.children[i].layoutTree) this.children[i].layoutTree();
            }
        },

        layout: function() {
            var d = this.d();
            if (this.children.length > 0) {
                var totalWidth = this.bounds.width - d.i.w;
                var individualWidth = totalWidth / this.children.length;
                for (var i = 0; i < this.children.length; i++) {
                    this.children[i].bounds = { x: (i * individualWidth) + d.i.l, y: d.i.t, width: individualWidth, height: this.bounds.height - d.i.h };
                }
            }
        },

        render: function() {
            this.layoutTree();
            this.repaint();
        }
    }
});