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


var Button = Class.define({
    type: "Button",

    superclass: Component,

    members: {
        init: function(parms) {
            this._super(parms);
        },

        paint: function(ctx) {
            var d = this.d();

            if (this.style.topImage && this.style.middleImage && this.style.bottomImage) {
                if (d.b.h >= this.style.topImage.height + this.style.bottomImage.height) {
                    ctx.drawImage(this.style.topImage, 0, 0);
                    if (d.b.h > this.style.topImage.height + this.style.bottomImage.height) {
                        ctx.drawImage(this.style.middleImage, 0, this.style.topImage.height, this.style.middleImage.width, d.b.h - this.style.topImage.height - this.style.bottomImage.height);
                    }
                    ctx.drawImage(this.style.bottomImage, 0, d.b.h - this.style.bottomImage.height);
                }
            } else if (this.style.backgroundImage) {
                ctx.drawImage(this.style.backgroundImage, 0, 0);
            } else {
                ctx.fillStyle = "red";
                ctx.fillRect(0, 0, d.b.w, d.b.h);
            }
        }
    }
});

var Scrollbar = Class.define({
    type: "Scrollbar",

    superclass: Container,

    members: {
        init: function(parms) {
            if (!parms) parms = {};
            this._super(parms);
            this.orientation = parms.orientation || TH.VERTICAL;
            this.value = parms.value || 0;
            this.min = parms.min || 0;
            this.max = parms.max || 100;
            this.extent = parms.extent || 0.1;
            this.increment = parms.increment || 2;

            this.up = new Button();
            this.down = new Button();
            this.bar = new Button();
            this.add([ this.up, this.down, this.bar ]);

            this.bus.bind("click", this.up, this.scrollup, this);
            this.bus.bind("click", this.down, this.scrolldown, this);
            this.bus.bind("mousedrag", this.bar, this.onmousedrag, this);
            this.bus.bind("mouseup", this.bar, this.onmouseup, this);
        },

        onmousedrag: function(e) {
            var currentPosition = (this.orientation == TH.VERTICAL) ? e.clientY : e.clientX;

            if (this.dragstart_value == undefined) {
                this.dragstart_value = this.value;
                this.dragstart_mouse = currentPosition;
                return;
            }

            var diff = currentPosition - this.dragstart_mouse;  // difference in pixels; needs to be translated to a difference in value

            var pixel_range = this.bounds.height - this.up.bounds.height - this.down.bounds.height - this.bar.bounds.height; // total number of pixels that map to the value range

            var pixel_to_value_ratio = (this.max - this.min) / pixel_range;

            this.value = this.dragstart_value + Math.floor(diff * pixel_to_value_ratio);
            if (this.value < this.min) this.value = this.min;
            if (this.value > this.max) this.value = this.max;
            if (this.scrollable) this.scrollable.scrollTop = this.value;
            this.render();
            if (this.scrollable) this.scrollable.repaint();
        },

        onmouseup: function(e) {
            delete this.dragstart_value;
            delete this.dragstart_mouse;
        },

        scrollup: function(e) {
            if (this.value > this.min) {
                this.value = Math.max(this.min, this.value - this.increment);
                if (this.scrollable) this.scrollable.scrollTop = this.value;
                this.render();
                if (this.scrollable) this.scrollable.repaint();
            }
        },

        scrolldown: function(e) {
            if (this.value < this.max) {
                this.value = Math.min(this.max, this.value + this.increment);
                if (this.scrollable) this.scrollable.scrollTop = this.value;
                this.render();
                if (this.scrollable) this.scrollable.repaint();
            }
        },

        layout: function() {
            var d = this.d();

            // check if there's a scrollable attached; if so, refresh state
            if (this.scrollable) {
                var view_height = this.scrollable.bounds.height;
                var scrollable_info = this.scrollable.getScrollInfo();
                this.min = 0;
                this.max = scrollable_info.scrollHeight - view_height;
                this.value = scrollable_info.scrollTop;
                this.extent = (scrollable_info.scrollHeight - view_height) / scrollable_info.scrollHeight;
            }

            // if the maximum value is less than the minimum, we're in an invalid state and won't paint anything
            if (this.max < this.min) {
                for (var i = 0; i < this.children.length; i++) delete this.children[i].bounds;
                return;
            }

            if (this.orientation == TH.VERTICAL) {
                var w = d.b.iw;
                var h = 12;
                this.up.bounds = { x: d.i.l + 1, y: d.i.t, width: w, height: h };
                this.down.bounds = { x: d.i.l + 1, y: d.b.ih - h, width: w, height: h };

                var scroll_track_height = d.b.ih - this.up.bounds.height - this.down.bounds.height;

                var extent_length = Math.min(Math.floor(scroll_track_height - (this.extent * scroll_track_height), d.b.ih - this.up.bounds.height - this.down.bounds.height));
                var extent_top = Math.floor(this.up.bounds.height + Math.min( (this.value / (this.max - this.min)) * (scroll_track_height - extent_length) ));
                this.bar.bounds = { x: d.i.l + 1, y: extent_top, width: d.b.iw, height: extent_length }
            } else {

            }
        },

        paint: function(ctx) {
            if (this.max < 0) return;

            // paint the track
            if (this.style.scrollTopImage) ctx.drawImage(this.style.scrollTopImage, 1, this.up.bounds.height);
            if (this.style.scrollMiddleImage) ctx.drawImage(this.style.scrollMiddleImage, 1, this.up.bounds.height + this.style.scrollTopImage.height, this.style.scrollMiddleImage.width, this.down.bounds.y - this.down.bounds.height - (this.up.bounds.x - this.up.bounds.height));
            if (this.style.scrollBottomImage) ctx.drawImage(this.style.scrollBottomImage, 1, this.down.bounds.y - this.style.scrollBottomImage.height);

            // propagate the styles to the children if not already there
            if (this.style.scrollHandleTopImage && !this.bar.style.topImage) {
                this.bar.style.topImage = this.style.scrollHandleTopImage;
                this.bar.style.middleImage = this.style.scrollHandleMiddleImage;
                this.bar.style.bottomImage = this.style.scrollHandleBottomImage;
                this.up.style.backgroundImage = this.style.scrollUpArrow;
                this.down.style.backgroundImage = this.style.scrollDownArrow;
            }

            this._super(ctx);
        }
    }
});

var Panel = Class.define({
    type: "Panel",

    superclass: Container,

    members: {
        paintSelf: function(ctx) {
            if (this.style.backgroundColor) {
                ctx.fillStyle = this.style.backgroundColor;

                var x = 0;
                var y = 0;
                var w = this.bounds.width;
                var h = this.bounds.height;

                ctx.fillRect(x, y, w, h);
            }
        }
    }
});

var ResizeNib = Class.define({
    type: "ResizeNib",

    superclass: Component,

    members: {
        init: function(parms) {
            this._super(parms);

            this.bus.bind("mousedown", this, this.onmousedown, this);
            this.bus.bind("mouseup", this, this.onmouseup, this);
            this.bus.bind("mousedrag", this, this.onmousedrag, this);
        },

        onmousedown: function(e) {
            this.startPos = { x: e.clientX, y: e.clientY};
        },

        onmousedrag: function(e) {
            if (this.startPos) {
                if (!this.firedDragStart) {
                    this.bus.fire("dragstart", this.startPos, this);
                    this.firedDragStart = true;
                }

                this.bus.fire("drag", { startPos: this.startPos, currentPos: { x: e.clientX, y: e.clientY } }, this);
            }
        },

        onmouseup: function(e) {
            if (this.startPos && this.firedDragStart) {
                this.bus.fire("dragstop", { startPos: this.startPos, currentPos: { x: e.clientX, y: e.clientY } }, this);
                delete this.firedDragStart;
            }
            delete this.startPos;
        },

        paint: function(ctx) {
            var d = this.d();

            if (this.attributes.orientation == TH.VERTICAL) {
                var bw = 7;
                var x = Math.floor((d.b.w / 2) - (bw / 2));
                var y = 7;

                ctx.fillStyle = "rgb(185, 180, 158)";
                for (var i = 0; i < 3; i++) {
                    ctx.fillRect(x, y, bw, 1);
                    y += 3;
                }

                y = 8;
                ctx.fillStyle = "rgb(10, 10, 8)";
                for (var i = 0; i < 3; i++) {
                    ctx.fillRect(x, y, bw, 1);
                    y += 3;
                }
            } else {
                var bh = 7;

                var dw = 8; // width of the bar area
                var dh = bh + 2; // height of the bar area

                var x = Math.floor(d.b.w / 2 - (dw / 2));
                var y = Math.floor(d.b.h / 2 - (dh / 2));

                // lay down the shadowy bits
                var cx = x;
                ctx.fillStyle = "rgba(0, 0, 0, 0.1)";
                for (var i = 0; i < 3; i++) {
                    ctx.fillRect(cx, y, 1, dh);
                    cx += 3;
                }

                // lay down the black shadow
                var cx = x + 1;
                ctx.fillStyle = "black";
                for (var i = 0; i < 3; i++) {
                    ctx.fillRect(cx, y + dh - 1, 1, 1);
                    cx += 3;
                }

                // draw the bars
                var cx = x + 1;
                ctx.fillStyle = "rgb(183, 180, 160)";
                for (var i = 0; i < 3; i++) {
                    ctx.fillRect(cx, y + 1, 1, bh);
                    cx += 3;
                }
            }

        }
    }
});

/*
    A "splitter" that visually demarcates areas of an interface. Can also have some "nibs" on its ends to facilitate resizing.
    Provides "dragstart", "drag", and "dragstop" events that are fired when a nib is dragged. Orientation is in terms of a container and
    is confusing; HORIZONTAL means the splitter is actually displayed taller than wide--what might be called vertically, and similarly
    VERTICAL means the splitter is wider than it is tall, i.e., horizontally. This is because the *container* is laid out such that
    different regions are stacked horizontally or vertically, and the splitter demarcates those areas.

    This bit of confusion was deemed better than having the orientation for a hierarchy of components be different but contributing to the
    same end.

    Note also that this component uses getPreferredHeight() and getPreferredWidth() differently than most; only one of the methods is
    valid for a particular orientation. I.e., when in HORIZONTAL orientation, getPreferredWidth() should be used and getPreferredHeight()
    ignored.

 */
var Splitter = Class.define({
    type: "Splitter",

    superclass: Container,

    members: {
        init: function(parms) {
            this._super(parms);

            this.topNib = new ResizeNib({ attributes: { orientation: this.attributes.orientation } });
            this.bottomNib = new ResizeNib({ attributes: { orientation: this.attributes.orientation } });
            this.add(this.topNib, this.bottomNib);

            this.label = parms.label;
            if (this.label) this.add(this.label);

            this.scrollbar = parms.scrollbar;
            if (this.scrollbar) this.add(this.scrollbar);

            this.bus.bind("drag", [ this.topNib, this.bottomNib ], this.ondrag, this);
            this.bus.bind("dragstart", [ this.topNib, this.bottomNib ], this.ondragstart, this);
            this.bus.bind("dragstop", [ this.topNib, this.bottomNib ], this.ondragstop, this);
        },

        ondrag: function(e) {
            this.bus.fire("drag", e, this);
        },

        ondragstart: function(e) {
            this.bus.fire("dragstart", e, this);
        },

        ondragstop: function(e) {
            this.bus.fire("dragstop", e, this);
        },

        getPreferredHeight: function(width) {
            return 20;
        },

        getPreferredWidth: function(height) {
            return 16;
        },

        layout: function() {
            var d = this.d();

            // if the orientation isn't explicitly set, guess it by examining the ratio
            if (!this.attributes.orientation) this.attributes.orientation = (this.bounds.height > this.bounds.width) ? TH.HORIZONTAL : TH.VERTICAL;

            if (this.attributes.orientation == TH.HORIZONTAL) {
                this.topNib.bounds = { x: 0, y: 0, height: d.b.w, width: d.b.w }
                this.bottomNib.bounds = { x: 0, y: this.bounds.height - d.b.w, height: d.b.w, width: d.b.w }

                if (this.scrollbar && this.scrollbar.shouldLayout()) {
                    this.scrollbar.bounds = { x: 0, y: this.topNib.bounds.height, height: d.b.h - (this.topNib.bounds.height * 2), width: d.b.w };
                }
            } else {
                this.topNib.bounds = { x: 0, y: 0, height: d.b.h, width: d.b.h }
                this.bottomNib.bounds = { x: d.b.w - d.b.h, y: 0, height: d.b.h, width: d.b.h }

                if (this.label) {
                    this.label.bounds = { x: this.topNib.bounds.x + this.topNib.bounds.width, y: 0, height: d.b.h, width: d.b.w - (d.b.h * 2) }
                }
            }
        },

        paintSelf: function(ctx) {
            var d = this.d();
            if (this.attributes.orientation == TH.VERTICAL) {
                ctx.fillStyle = "rgb(73, 72, 66)";
                ctx.fillRect(0, 0, d.b.w, 1);
                ctx.fillStyle = "black";
                ctx.fillRect(0, d.b.h - 1, d.b.w, 1);

                var gradient = ctx.createLinearGradient(0, 1, 0, d.b.h - 1);
                gradient.addColorStop(0, "rgb(50, 48, 42)");
                gradient.addColorStop(1, "rgb(22, 22, 19)");
                ctx.fillStyle = gradient;
                ctx.fillRect(0, 1, d.b.w, d.b.h - 2);
            } else {
                ctx.fillStyle = "rgb(105, 105, 99)";
                ctx.fillRect(0, 0, 1, d.b.h);
                ctx.fillStyle = "black";
                ctx.fillRect(d.b.w - 1, 0, 1, d.b.h);

                var gradient = ctx.createLinearGradient(1, 0, d.b.w - 2, 0);
                gradient.addColorStop(0, "rgb(56, 55, 49)");
                gradient.addColorStop(1, "rgb(62, 61, 55)");
                ctx.fillStyle = gradient;
                ctx.fillRect(1, 0, d.b.w - 2, d.b.h);
            }
        }
    }
});

var SplitPanelContainer = Class.define({
    type: "SplitPanelContainer",

    superclass: Panel,

    members: {
        init: function(parms) {
            this._super(parms);

            this.splitter = new Splitter({ attributes: { orientation: this.attributes.orientation }, label: parms.label });
        },

        getContents: function() {
            var childrenWithoutSplitter = this.children.without(this.splitter);
            if (childrenWithoutSplitter.length > 0) return childrenWithoutSplitter[0];
        },

        layout: function() {
            var childrenWithoutSplitter = this.children.without(this.splitter);
            if (this.children.length == childrenWithoutSplitter.length) this.add(this.splitter);

            var slength = (this.attributes.orientation == TH.HORIZONTAL) ?
                          this.splitter.getPreferredWidth(this.bounds.height) :
                          this.splitter.getPreferredHeight(this.bounds.width);
            if (this.splitter.shouldLayout()) {
                if (this.attributes.orientation == TH.HORIZONTAL) {
                    this.splitter.bounds = { x: this.bounds.width - slength, y: 0, height: this.bounds.height, width: slength };
                } else {
                    this.splitter.bounds = { x: 0, y: this.bounds.height - slength, height: slength, width: this.bounds.width };
                }
            } else {
                slength = 0;
            }

            // only the first non-splitter child is laid out
            if (childrenWithoutSplitter.length > 0) {
                if (this.attributes.orientation == TH.HORIZONTAL) {
                    childrenWithoutSplitter[0].bounds = { x: 0, y: 0, height: this.bounds.height, width: this.bounds.width - slength }
                } else {
                    childrenWithoutSplitter[0].bounds = { x: 0, y: 0, height: this.bounds.height - slength, width: this.bounds.width }
                }
            }
        }
    }
});

/*
    A component that allocates all visible space to two or more nested regions.
 */
var SplitPanel = Class.define({
    type: "SplitPanel",

    superclass: Panel,

    uses: [
        StringHelpers
    ],

    members: {
        init: function(parms) {
            this._super(parms);

            if (!this.attributes.orientation) this.attributes.orientation = TH.HORIZONTAL;

            if (!this.attributes.regions) this.attributes.regions = [{},{}];
        },

        ondragstart: function(e) {
            var container = e.thComponent.parent; // splitter -> splitpanecontainer
            container.region.startSize = container.region.size;
        },

        ondrag: function(e) {
            var container = e.thComponent.parent; // splitter -> splitpanecontainer

            var delta = (this.attributes.orientation == TH.HORIZONTAL) ? e.currentPos.x - e.startPos.x : e.currentPos.y - e.startPos.y;

            container.region.size = container.region.startSize + delta;
            this.render();
        },

        ondragstop: function(e) {
            var container = e.thComponent.parent; // splitter -> splitpanecontainer
            delete container.region.startSize;
        },

        layout: function() {
            this.remove(this.children); // remove any of the existing region panels

            /*
               iterate through each region, performing a couple of tasks:
                - create a container for each region if it doesn't already have one
                - put the value of the contents property of region into the container if necessary
                - hide the splitter on the last region
             */
            for (var i = 0; i < this.attributes.regions.length; i++) {
                var region = this.attributes.regions[i];
                if (!region.container) {
                    region.container = new SplitPanelContainer({ attributes: { orientation: this.attributes.orientation }, label: region.label });

                    region.container.region = region;   // give the container a reference back to the region

                    // capture the start size of the region when the nib's drag starts
                    this.bus.bind("dragstart", region.container.splitter, this.ondragstart, this);
                    this.bus.bind("drag", region.container.splitter, this.ondrag, this);
                    this.bus.bind("dragstop", region.container.splitter, this.ondragstop, this);
                }

                // update the content panel for the split panel container
                if (region.contents && (region.contents != region.container.getContents())) {
                    region.container.removeAll();
                    region.container.add(region.contents);
                }

                // make the last container's splitter invisible
                if (i == this.attributes.regions.length - 1) region.container.splitter.style.display = "none";

                this.add(region.container);
            }

            var containerSize = (this.attributes.orientation == TH.HORIZONTAL) ? this.bounds.width : this.bounds.height;

            // size the regions
            var totalSize = 0;
            for (var i = 0; i < this.attributes.regions.length; i++) {
                var r = this.attributes.regions[i];

                if (!r.size) {
                    r.size = (this.attributes.defaultSize || (100 / this.attributes.regions.length) + "%");
                }

                if (this.isPercentage(r.size)) {
                    // percentage lengths are allowed, but will be immediately converted to pixels
                    r.size = Math.floor((parseInt(r.size) / 100) * containerSize);
                }

                // enforce a minimum width
                if (r.size < 30) r.size = 30;

                totalSize += r.size;
            }
            if (totalSize > containerSize) {   // if the regions are bigger than the split pane size, shrink 'em, right-to-left
                var diff = totalSize - containerSize;
                for (var i = this.attributes.regions.length - 1; i >= 0; i--) {
                    var r = this.attributes.regions[i];

                    var originalSize = r.size;
                    r.size -= diff;
                    if (r.size < 30) r.size = 30;
                    diff -= (originalSize - r.size);

                    if (diff <= 0) break;
                }
            } else if (totalSize < containerSize) {    // if the regions are smaller, grow 'em, all in the last one
                var r = this.attributes.regions[this.attributes.regions.length - 1].size += (containerSize - totalSize);
            }

            var startPx = 0;
            for (var i = 0; i < this.attributes.regions.length; i++) {
                var region = this.attributes.regions[i];
                if (this.attributes.orientation == TH.HORIZONTAL) {
                    region.container.bounds = { x: startPx, y: 0, width: region.size, height: this.bounds.height };
                } else {
                    region.container.bounds = { x: 0, y: startPx, width: this.bounds.width, height: region.size };
                }
                startPx += region.size;

            }
        }
    }
});

var Label = Class.define({
    type: "Label",

    superclass: Panel,

    members: {
        init: function(parms) {
            if (!parms) parms = {};
            this._super(parms);
            if (!this.border) this.border = new EmptyBorder({ insets: { left: 5, right: 5, top: 2, bottom: 2 }});
            this.attributes.text = parms.text || "";
            if (!this.style.font) this.style.font = "12pt Arial";
            if (!this.style.color) this.style.color = "black";
        },

        styleContext: function(ctx) {
            if (!ctx) return;

            ctx.font = this.style.font;
            ctx.fillStyle = this.style.color;
            
            return ctx;
        },

        getPreferredWidth: function(height) {
            var ctx = this.styleContext(this.parent.getScratchContext());

            // the +2 is to compensate for anti-aliasing on Windows, which isn't taken into account in measurements; this fudge factor should eventually become platform-specific
            var w = ctx.measureText(this.attributes.text).width + 2;
            return w + this.getInsets().left + this.getInsets().right;
        },

        getPreferredHeight: function(width) {
            var ctx = this.styleContext(this.parent.getScratchContext());
            var h = Math.floor(ctx.measureText(this.attributes.text).ascent * 1.5);   // multiplying by 2 to fake a descent and leading
            return h + this.getInsets().top + this.getInsets().bottom;
        },

        paint: function(ctx) {
            var d = this.d();

            if (this.style.backgroundColor) this._super(ctx);

            this.styleContext(ctx);

            var textMetrics = ctx.measureText(this.attributes.text);

            var textToRender = this.attributes.text;
            var lastLength = textToRender.length - 2;
            while (textMetrics.width > (d.b.w - d.i.w)) {
                if (lastLength == 0) {
                    textToRender = "...";
                    break;
                }

                var left = Math.floor(lastLength / 2);
                var right = left + (lastLength % 2);
                textToRender = this.attributes.text.substring(0, left) + "..." + this.attributes.text.substring(this.attributes.text.length - right);
                textMetrics = ctx.measureText(textToRender);

                lastLength -= 1;
            }

            ctx.fillText(textToRender, this.getInsets().left, this.getInsets().top + textMetrics.ascent);
        }
    }
});

var ExpandingInfoPanel = Class.define({
    type: "ExpandingInfoPanel",

    superclass: Panel,

    members: {
        init: function(parms) {
            this._super(parms);
        },

        getMinimumRowHeight: function() {
            return 40;
        },

        getMinimumColumnWidth: function() {
            
        },

        layout: function() {
            if (this.children.length == 0) return;

            var d = this.d();


            var rows = Math.floor(Math.sqrt(this.children.length));
            var height = Math.floor(d.b.h / rows);
            while (height < this.getMinimumRowHeight() && rows > 1) {
                rows--;
                height = Math.floor(d.b.h / rows); 
            }


            var perRow = Math.floor(this.children.length / rows);
            var remainder = this.children.length % rows;

            // TODO: verify a minimum height (and perhaps width)

            var currentChild = 0;
            var heightRemainder = d.b.h % rows;

            var currentY = 0;
            for (var i = 0; i < rows; i++) {
                var h = (i == rows - 1) ? height + heightRemainder : height;

                var cols = (remainder > 0) ? perRow + 1 : perRow;
                remainder--;

                var width = Math.floor(d.b.w / cols);
                var widthRemainder = d.b.w % cols;

                var currentX = 0;
                for (var z = 0; z < cols; z++) {
                    var w = (z == cols - 1) ? width + widthRemainder : width;
                    this.children[currentChild++].bounds = { x: currentX, y: currentY, width: w, height: h };
                    currentX += w;
                }
                currentY += h;
            }
        }
    }
});

var List = Class.define({
    type: "List",

    superclass: Container,

    members: {
        init: function(parms) {
            if (!parms) parms = {};
            this._super(parms);

            this.items = parms.items || [];

            this.scrollTop = 0;

            this.bus.bind("mousedown", this, this.onmousedown, this);
        },

        onmousedown: function(e) {
            var item = this.getItemForPosition({ x: e.componentX, y: e.componentY });
            if (item != this.selected) {
                if (item) this.selected = item; else delete this.selected;
                this.bus.fire("itemselected", { container: this, item: this.selected }, this); 
                this.repaint();
            }
        },

        getItemForPosition: function(pos) {
            pos.y += this.scrollTop;
            var y = this.getInsets().top;
            for (var i = 0; i < this.items.length; i++) {
                var h = this.heights[i];
                if (pos.y >= y && pos.y <= y + h) return this.items[i];
                y += h;
            }
        },

        getRenderer: function(rctx) {
            this.renderer.attributes.text = rctx.item.toString();
            this.renderer.style.font = this.style.font;
            this.renderer.style.color = this.style.color;
            this.renderer.selected = rctx.selected;
            this.renderer.item = rctx.item;
            return this.renderer;
        },

        renderer: new Label({ style: { border: new EmptyBorder({ size: 3 }) } }),

        getRenderContext: function(item, row) {
            return { item: item, even: row % 2 == 0, selected: this.selected == item };
        },

        getRowHeight: function() {
            if (!this.rowHeight) {
                var d = this.d();
                var firstItem = (this.items.length > 0) ? this.items[0] : undefined;
                if (firstItem) {
                    var renderer = this.getRenderer(this.getRenderContext(firstItem, 0));
                    this.add(renderer);
                    this.rowHeight = renderer.getPreferredHeight(d.b.w - d.i.w);
                    this.remove(renderer);
                }
            }
            return this.rowHeight || 0;
        },

        getScrollInfo: function() {
            return { scrollTop: this.scrollTop, scrollHeight: this.getRowHeight() * this.items.length }
        },

        paint: function(ctx) {
            var d = this.d();
            var paintHeight = Math.max(this.getScrollInfo().scrollHeight, d.b.h);
            var scrollInfo = this.getScrollInfo();

            ctx.save();
            ctx.translate(0, -this.scrollTop);

            try {
                if (this.style.backgroundColor) {
                    ctx.fillStyle = this.style.backgroundColor;
                    ctx.fillRect(0, 0, d.b.w, paintHeight);
                }

                if (this.style.backgroundColorOdd) {
                    var rowHeight = this.rowHeight;
                    if (!rowHeight) {
                        var firstItem = (this.items.length > 0) ? this.items[0] : undefined;
                        if (firstItem) {
                            var renderer = this.getRenderer(this.getRenderContext(firstItem, 0));
                            this.add(renderer);
                            rowHeight = renderer.getPreferredHeight(d.b.w - d.i.w);
                            this.remove(renderer);
                        }
                    }
                    if (rowHeight) {
                        var y = d.i.t + rowHeight;
                        ctx.fillStyle = this.style.backgroundColorOdd;
                        while (y < paintHeight) {
                            ctx.fillRect(d.i.l, y, d.b.w - d.i.w, rowHeight);
                            y += rowHeight * 2;
                        }
                    }
                }

                if (this.items.length == 0) return;

                if (!this.renderer) {
                    console.log("No renderer for List of type " + this.type + " with id " + this.id + "; cannot paint contents");
                    return;
                }


                this.heights = [];
                var y = d.i.t;
                for (var i = 0; i < this.items.length; i++) {
                    var stamp = this.getRenderer(this.getRenderContext(this.items[i], i));
                    if (!stamp) break;

                    this.add(stamp);

                    var w = d.b.w - d.i.w;
                    var h = (this.rowHeight) ? this.rowHeight : stamp.getPreferredHeight(w);
                    this.heights.push(h);
                    stamp.bounds = { x: 0, y: 0, height: h, width: w };

                    ctx.save();
                    ctx.translate(d.i.l, y);
                    ctx.beginPath();
                    ctx.rect(0, 0, w, h);
                    ctx.closePath();
                    ctx.clip();

                    stamp.paint(ctx);

                    ctx.restore();

                    this.remove(stamp);

                    y+= h;
                }
            } finally {
                ctx.restore();
            }
        }
    }
});

var HorizontalTree = Class.define({
    type: "HorizontalTree",

    superclass: Container,

    members: {
        init: function(parms) {
            if (!parms) parms = {};
            this._super(parms);
            if (!this.style.defaultSize) this.style.defaultSize = 150;

            this.attributes.orientation = TH.HORIZONTAL;

            this.lists = [];
            this.splitters = [];
        },

        setData: function(data) {
            for (var i = 0; i < this.lists.length; i++) {
                this.remove(this.lists[i]);
                this.remove(this.splitters[i]);
                this.bus.unbind(this.lists[i]);
                this.bus.unbind(this.splitters[i]);
            }
            this.lists = [];
            this.splitters = [];

            this.data = data;
            this.showChildren(null, data);
        },

        ondragstart: function(e) {
            var splitterIndex = this.splitters.indexOf(e.thComponent);
            this.startSize = this.listWidths[splitterIndex];
        },

        ondrag: function(e) {
            var splitterIndex = this.splitters.indexOf(e.thComponent);
            var delta = (this.attributes.orientation == TH.HORIZONTAL) ? e.currentPos.x - e.startPos.x : e.currentPos.y - e.startPos.y;
            this.listWidths[splitterIndex] = this.startSize + delta;
            this.render();
        },

        ondragstop: function(e) {
            delete this.startSize;
        },

        updateData: function(parent, contents) {
            parent.contents = contents;
            if (this.getSelectedItem() == parent) {
                this.showChildren(parent, parent.contents);
            }
        },

        showChildren: function(newItem, children) {
            if (this.details) {
                this.remove(this.details);
                delete this.details;
            }

            if (!Object.isArray(children)) {
                // if it's not an array, assume it's a function that will load the children
                children(this.getSelectedPath(), this);
                this.render();
                return;
            }

            if (!children || children.length == 0) return;
            var list = this.createList(children);
            list.id = "list " + (this.lists.length + 1);

            this.bus.bind("click", list, this.itemSelected, this);
            var tree = this;
            this.bus.bind("dblclick", list, function(e) {
                tree.bus.fire("dblclick", e, tree);
            });
            this.lists.push(list);
            this.add(list);

            var splitter = new Splitter({ attributes: { orientation: TH.HORIZONTAL }, scrollbar: new Scrollbar() });
            splitter.scrollbar.style = this.style;
            splitter.scrollbar.scrollable = list;
            splitter.scrollbar.opaque = false;
            this.bus.bind("dragstart", splitter, this.ondragstart, this);
            this.bus.bind("drag", splitter, this.ondrag, this);
            this.bus.bind("dragstop", splitter, this.ondragstop, this);
            
            this.splitters.push(splitter);
            this.add(splitter);

            if (this.parent) this.render();
        },

        showDetails: function(item) {
            if (this.details) this.remove(this.details);

//            var panel = new Panel({ style: { backgroundColor: "white" } });
//            var label = new Label({ text: "Some details, please!" });
//            panel.add(label);
//            this.details = panel;
//            this.add(this.details);

            if (this.parent) this.repaint();
        },

        createList: function(items) {
            var list = new List({ items: items, style: this.style });
            if (this.renderer) list.renderer = this.renderer;
            list.oldGetRenderer = list.getRenderer;
            list.getRenderer = function(rctx) {
                var label = list.oldGetRenderer(rctx);
                label.attributes.text = rctx.item.name;
                return label;
            }
            return list;
        },

        getSelectedItem: function() {
            var selected = this.getSelectedPath();
            if (selected.length > 0) return selected[selected.length - 1];
        },

        getSelectedPath: function() {
            var path = [];

            for (var i = 0; i < this.lists.length; i++) {
                if (this.lists[i].selected) {
                    path.push(this.lists[i].selected);
                } else {
                    break;
                }
            }

            return path;
        },

        itemSelected: function(e) {
            var list = e.thComponent;

            // add check to ensure that list has an item selected; otherwise, bail
            if (!list.selected) return; 

            var path = [];

            for (var i = 0; i < this.lists.length; i++) {
                path.push(this.lists[i].selected);
                if (this.lists[i] == list) break;
            }

            if (path.length < this.lists.length) {
                // user selected an item in a previous list; must ditch the subsequent lists
                var newlists = this.lists.slice(0, path.length);
                var newsplitters = this.splitters.slice(0, path.length);
                for (var z = path.length; z < this.lists.length; z++) {
                    this.bus.unbind(this.lists[z]);
                    this.bus.unbind(this.splitters[z]);

                    this.remove(this.lists[z]);
                    this.remove(this.splitters[z]);
                }
                this.lists = newlists;
                this.splitters = newsplitters;
            }

            // determine whether to display new list of children or details of selection
            var newItem = this.getItem(path);
            if (newItem && newItem.contents) {
                this.showChildren(newItem, newItem.contents);
            } else {
                this.showDetails(newItem);
            }
        },

        getItem: function(pathToItem) {
            var items = this.data;
            var item;
            for (var i = 0; i < pathToItem.length; i++) {
                for (var z = 0; z < items.length; z++) {
                    if (items[z] == pathToItem[i]) {
                        item = items[z];
                        items = item.contents;
                        break;
                    }
                }
            }
            return item;
        },

        layout: function() {
            var d = this.d();

            var x = d.i.l;
            for (var i = 0; i < this.lists.length; i++) {
                var list = this.lists[i];
                if (!this.listWidths) this.listWidths = [];
                if (!this.listWidths[i]) this.listWidths[i] = this.style.defaultSize;
                var w = this.listWidths[i];
                list.bounds = { x: x, y: d.i.t, width: w, height: d.b.h - d.i.h };

                x += w;

                var splitter = this.splitters[i];
                w = splitter.getPreferredWidth(-1);
                splitter.bounds = { x: x, y: d.i.t, width: w, height: d.b.h - d.i.h };
                x += w;

            }

            if (this.details) {
                this.details.bounds = { x: x, y: d.i.t, width: 150, height: d.b.h - d.i.h };
            }
        },

        paintSelf: function(ctx) {
            var d = this.d();

            if (this.style.backgroundColor) {
                ctx.fillStyle = this.style.backgroundColor;
                ctx.fillRect(0, 0, d.b.w, d.b.h);
            }
        }
    }
});
