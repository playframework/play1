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

var heightDiff;
var projects;
var scene;
var tree;
var infoPanel;
var go = Bespin.Navigate; // short cut static method

function sizeCanvas(canvas) {
    if (!heightDiff) {
        heightDiff = $("header").clientHeight + $("subheader").clientHeight + $("footer").clientHeight;
    }
    var height = window.innerHeight - heightDiff + 11;
    canvas.writeAttribute({ width: window.innerWidth, height: height });
}

Event.observe(window, "resize", function() {
    sizeCanvas($("canvas"));
});

Event.observe(document, "dom:loaded", function() {
    sizeCanvas($("canvas"));

    $('subheader', 'header').invoke('enableTextSelection', false);
    
    Bespin.displayVersion(); // display the version on the page

    scene = new Scene($("canvas"));

    tree = new HorizontalTree({ style: { backgroundColor: "rgb(76, 74, 65)",
                                         backgroundColorOdd: "rgb(82, 80, 71)",
                                         font: "9pt Tahoma",
                                         color: "white",
                                         scrollTopImage: $("vscroll_track_top"),
                                         scrollMiddleImage: $("vscroll_track_middle"),
                                         scrollBottomImage: $("vscroll_track_bottom"),
                                         scrollHandleTopImage: $("vscroll_top"),
                                         scrollHandleMiddleImage: $("vscroll_middle"),
                                         scrollHandleBottomImage: $("vscroll_bottom"),
                                         scrollUpArrow: $("vscroll_up_arrow"),
                                         scrollDownArrow: $("vscroll_down_arrow")
                                         }});

    var renderer = new Label({ style: { border: new EmptyBorder({ size: 3 }) } });
    renderer.old_paint = renderer.paint;
    renderer.paint = function(ctx) {
        var d = this.d();

        if (this.selected) {
            ctx.fillStyle = "rgb(177, 112, 20)";
            ctx.fillRect(0, 0, d.b.w, 1);

            var gradient = ctx.createLinearGradient(0, 0, 0, d.b.h);
            gradient.addColorStop(0, "rgb(172, 102, 1)");
            gradient.addColorStop(1, "rgb(219, 129, 1)");
            ctx.fillStyle = gradient;
            ctx.fillRect(0, 1, d.b.w, d.b.h - 2);

            ctx.fillStyle = "rgb(160, 95, 1)";
            ctx.fillRect(0, d.b.h - 1, d.b.w, 1);
        }

        if (this.item.contents) {
            renderer.styleContext(ctx);
            var metrics = ctx.measureText(">");
            ctx.fillText(">", d.b.w - metrics.width - 5, d.b.h / 2 + (metrics.ascent / 2) - 1);
        }

        this.old_paint(ctx);
    }
    tree.renderer = renderer;

    projects = new BespinProjectPanel();

    var topPanel = new Panel();
    topPanel.add([ projects, tree ]);
    topPanel.layout = function() {
        var d = this.d();
        projects.bounds = { x: d.i.l, y: d.i.t, width: projects.getPreferredWidth(d.b.h - d.i.h), height: d.b.h - d.i.h };
        tree.bounds = { x: projects.bounds.x + projects.bounds.width, y: d.i.t, width: d.b.w - d.i.w - projects.bounds.width, height: d.b.h - d.i.h };
    }
    projects.list.renderer = renderer;

    infoPanel = new ExpandingInfoPanel({ style: { backgroundColor: "rgb(61, 59, 52)" } });

    var splitPanel = new SplitPanel({ id: "splitPanel", attributes: {
        orientation: TH.VERTICAL,
        regions: [ { size: "75%", contents: topPanel }, { size: "25%", contents: infoPanel } ]
    } });

    splitPanel.attributes.regions[0].label = new Label({
            id: "foobar",
            text: "Open Sessions",
            style: {
                color: "white",
                font: "9pt Tahoma"
            },
            border: new EmptyBorder({ size: 4 })
    });

    scene.root.add(splitPanel);

    scene.render();

    scene.bus.bind("dblclick", tree, function(e) {
        var newTab = e.shiftKey;
        var path = tree.getSelectedPath();
        if (path.length == 0 || path.last().contents) return; // don't allow directories either
        go.editor(currentProject, getFilePath(path), newTab);
    });

    scene.bus.bind("itemselected", projects.list, function(e) {
        currentProject = e.item;
        _server.list(e.item, null, displayFiles);
    });
    
    // setup the command line
    _server      = new Bespin.Server('/bespin');
    _settings    = new Bespin.Settings.Core();
    _files       = new Bespin.FileSystem();
    _commandLine = new Bespin.CommandLine.Interface($('command'), Bespin.Commands.Dashboard);
    
    // Handle jumping to the command line
    Event.observe(document, "keydown", function(e) {
        var handled = _commandLine.handleCommandLineFocus(e);
        if (handled) return false;
    });

    // get logged in name; if not logged in, display an error of some kind
    _server.currentuser(loggedIn, notLoggedIn);
});

var currentProject;

// After a project is imported or created, do a list
document.observe("bespin:project:imported", function(event) {
    _server.list(null, null, displayProjects); // get projects
});
document.observe("bespin:project:set", function(event) {
    _server.list(null, null, displayProjects); // get projects
});

function loggedIn(user) {
    _server.list(null, null, displayProjects); // get projects
    _server.listOpen(displaySessions); // get sessions
}

function notLoggedIn(xhr) {
    go.home();
}

function displayFiles(files) {
    tree.setData(prepareFilesForTree(files));
    tree.render();
}

function prepareFilesForTree(files) {
    if (files.length == 0) return [];

    var fdata = [];
    for (var i = 0; i < files.length; i++) {
		var name = files[i].name;
        if (name.endsWith("/")) {
            var name = name.substring(0, name.length - 1);
            var contents = fetchFiles;
            fdata.push({ name: name, contents: contents });
        } else {
            fdata.push({ name: name });
        }
    }

    return fdata;
}

function getFilePath(treePath) {
    var filepath = "";
    for (var i = 0; i < treePath.length; i++) {
        if (treePath[i] && treePath[i].name)
            filepath += treePath[i].name + ((i < treePath.length - 1) ? "/" : "");
    }
    return filepath;
}

function fetchFiles(path, tree) {
    var filepath = currentProject + "/" + getFilePath(path);

    _server.list(filepath, null, function(files) {
        tree.updateData(path[path.length - 1], prepareFilesForTree(files));
        tree.render();
    });
}

function displaySessions(sessions) {
    infoPanel.removeAll();

    for (var project in sessions) {
        for (var file in sessions[project]) {
            var lastSlash = file.lastIndexOf("/");
            var path = (lastSlash == -1) ? "" : file.substring(0, lastSlash);
            var name = (lastSlash == -1) ? file : file.substring(lastSlash + 1);

            var panel = new BespinSessionPanel({ filename: name, project: project, path: path });
            infoPanel.add(panel);
            panel.bus.bind("dblclick", panel, function(e) {
                var newTab = e.shiftKey;
                go.editor(e.thComponent.session.project, e.thComponent.session.path + "/" + e.thComponent.session.filename, newTab);
            });
        }
    }
    infoPanel.render();

//    setTimeout(function() {
//        _server.listOpen(displaySessions);   // get sessions
//    }, 3000);
}

function displayProjects(projectItems) {
    for (var i = 0; i < projectItems.length; i++) {
        projectItems[i] = projectItems[i].name.substring(0, projectItems[i].name.length - 1);
    }
    projects.list.items = projectItems;
    scene.render();
}
