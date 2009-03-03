//  ***** BEGIN LICENSE BLOCK *****
// Version: MPL 1.1
// 
// The contents of this file are subject to the Mozilla Public License  
// Version
// 1.1 (the "License"); you may not use this file except in compliance  
// with
// the License. You may obtain a copy of the License at
// http://www.mozilla.org/MPL/
// 
// Software distributed under the License is distributed on an "AS IS"  
// basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied. See the  
// License
// for the specific language governing rights and limitations under the
// License.
// 
// The Original Code is Bespin.
// 
// The Initial Developer of the Original Code is Mozilla.
// Portions created by the Initial Developer are Copyright (C) 2009
// the Initial Developer. All Rights Reserved.
// 
// Contributor(s):
// 
// ***** END LICENSE BLOCK *****
// 

var EventHelpers = new Trait({
    methods: {
        // only works on a Scene at the moment as it uses  
        wrapEvent: function(e, root) {
            // compute the canvas-local coordinates
            var x = e.clientX - this.canvas.cumulativeOffset().left;
            var y = e.clientY - this.canvas.cumulativeOffset().top;

            var component = root.getComponentForPosition(x, y, true);
            e.thComponent = component;

            this.addComponentXY(e, root, component);
        },

        // changes clientX and clientY from space of source to space of dest; no change wraught if dest incompatible with source
        addComponentXY: function(e, source, dest) {
            if (!dest.bounds) {
                console.log("No dest bounds - " + dest.type);
                console.log(dest.bounds);
                console.log(dest);
                return;
            }

            // compute the canvas-local coordinates
            var x = e.clientX - this.canvas.cumulativeOffset().left;
            var y = e.clientY - this.canvas.cumulativeOffset().top;

            var nxy = { x: x, y: y };

            var c = dest;
            while (c) {
                nxy.x -= c.bounds.x;
                nxy.y -= c.bounds.y;
                c = c.parent;

                if (c == source) {
                    e.componentX = nxy.x;
                    e.componentY = nxy.y;
                    return;
                }
            }
        }
    }
});

var StringHelpers = new Trait({
    methods: {
        isPercentage: function(value) {
            return value.indexOf && value.indexOf("%") != -1;    // TODO: make more robust
        }
    }
});

var ComponentHelpers = new Trait({
    methods: {
        // returns hash with some handy short-cuts for painting
        d: function() {
            return {
               b: (this.bounds) ? { x: this.bounds.x, y: this.bounds.y, w: this.bounds.width, h: this.bounds.height,
                                    iw: this.bounds.width - this.getInsets().left - this.getInsets().right,
                                    ih: this.bounds.height - this.getInsets().top - this.getInsets().bottom } : {},
               i: { l: this.getInsets().left, r: this.getInsets().right, t: this.getInsets().top, b: this.getInsets().bottom,
                    w: this.getInsets().left + this.getInsets().right, h: this.getInsets().top + this.getInsets().bottom }
            }
        },

        shouldPaint: function() {
            return (this.shouldLayout() && this.style.visibility != "hidden");
        },

        shouldLayout: function() {
            return (this.style.display != "none");
        },

        emptyInsets: function() {
            return { left: 0, right: 0, bottom: 0, top: 0 };
        }
    }
});

var ContainerHelpers = new Trait({
    methods: {
        getScene: function() {
            var container = this;
            while (!container.scene && container.parent) container = container.parent;
            return container.scene;
        },

        getChildById: function(id) {
            for (var i = 0; i < this.children.length; i++) {
                if (this.children[i].id == id) return this.children[i];
            }
        },

        getComponentForPosition: function(x, y, recurse) {
            for (var i = 0; i < this.children.length; i++) {
                if (!this.children[i].bounds) continue;
                
                if (this.children[i].bounds.x <= x && this.children[i].bounds.y <= y
                        && (this.children[i].bounds.x + this.children[i].bounds.width) >= x
                        && (this.children[i].bounds.y + this.children[i].bounds.height) >= y) {
                    if (!recurse) return this.children[i];
                    return (this.children[i].getComponentForPosition) ?
                           this.children[i].getComponentForPosition(x - this.children[i].bounds.x, y - this.children[i].bounds.y, recurse) :
                           this.children[i];
                }
            }
            return this;
        },

        removeAll: function() {
            this.remove(this.children);
        }
    }
});