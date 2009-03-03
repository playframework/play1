/*
 * Copyright (c) 2008, Joseph Hurst. All rights reserved.
 * Code licensed under the MIT License:
 * http://www.opensource.org/licenses/mit-license.php
 * Version 0.0.1-**forked**
 *
 * Class definition code based on Dean Edward's Base2 (MIT License) and work by
 * John Resig.
 *
 * NOTE: modifications have been made. -ben
 */
/** @ignore */
(function(){
  ////////////////////////////////////////////////////////////////////////////
  //
  // Classes
  //
  ////////////////////////////////////////////////////////////////////////////

  // Class definition borrows heavily from John Resig's blog post at:
  // http://ejohn.org/blog/simple-javascript-inheritance/
  // This in turn borrows heavily from Dean Edward's Base 2:
  // http://code.google.com/p/base2/
  // Class definition is secondary to the core traits functionality, which
  // is the focus of this project.  Mssr. Resig's class implementation was
  // chosen to support traits because it's concise and adds a 'super' reference
  // to 'this', which allows trait methods to more effectively preserve the
  // "flattening property" as described in the traits research paper.
  //
  // Just about any JavaScript implementation of pseudo-classical inheritance
  // could be plugged in here (YUI's, Tibco GI's, Ext's, etc.) with little
  // difficulty.


  // initializing is a flag used in the constructor to indicate that we want
  // to create a dummy instance of a class for the purpose of prototype
  // chaining.  We avoid a normal constructor call because executing the full
  // constructor may have expensive and undesired side effects.
  var initializing = false;

  // fnTest is applied to methods to check if they have references to _super.
  // The machinery used to hook up _super is slightly expensive, so we want
  // to avoid enabling it when possible.
  // Unfortunately, not all JS implementations will return the source code
  // of a function when casted to a String, so we do a capability test here.
  // JS implementations that can not inspect source code will always set up
  // _super in method calls while the better implementations will only set up
  // _super if the word _super is found in the source code.  Note that this
  // means _super won't be set up properly if you do something silly like
  // this["_sup"+"er"]()
  var fnTest = /xyz/.test(function(){xyz;}) ?
                 /\b_super\b/ :
                 { test: function(){return true;} };

  /**
   * Global namespace for class related functions.
   * @namespace
   * @name Class
   */
  this.Class = function(){
      this.type = "Class";
  };

  /**
   * Define a new class.  In the traits model of object oriented programming
   * a class consists of:
   * <ul>
   * <li>a superclass</li>
   * <li>a set of composed traits</li>
   * <li>a collection of methods and state variables</li>
   * </ul>
   * This structure is directly mirrored in the argument structure of
   * Class.define.
   *
   * <p>
   * A number of special properties are assigned to the class at definition
   * time:
   * </p>
   * <ul>
   * <li>A static reference 'superclass' is added which points at the
   * superclass's constructor function.
   * </li>
   * <li>The static 'constructor' reference is pointed at the class itself so
   * that the 'typeof' and 'instanceof' operators will behave as expected.
   * </li>
   * <li>A static and instance method 'does' is added as well.  This method
   * behaves just like {@link Trait#does} with the notable difference that it
   * also includes any superclasses's traits in its output.
   * </li>
   * <li>Finally, an instance method '_super' is added such that invoking it in
   * any other instance method will call the first function with the same name
   * defined in the class's superclass prototype chain.
   * </li>
   * </ul>
   *
   * <p>
   * Conflicts among instance properties are resolved in the following order:
   * class members override trait members and superclass members, and trait
   * members override superclass members.
   * </p>
   *
   * <p>
   * The class constructor is specified as a method named init passed into the
   * o.members argument.
   * </p>
   *
   * @example
   * var HappyFace = Class.define({
   *   superclass: Doodle,
   *   uses: [
   *     TFace,
   *     TColoredCircle.aliases({drawColoredCircle: 'draw'})
   *   ],
   *   members: {
   *     // constructor
   *     init: function(color) {
   *       this.isDrawn = false;
   *       if (color)
   *         this.setColor(color);
   *     },
   *     // draw a happy face
   *     draw: function() {
   *       // call Doodle's draw method to set up the canvas
   *       this._super();
   *       // draw a colored circle
   *       this.drawColoredCircle();
   *       // draw the face
   *       this.drawEyes();
   *       this.drawMouth():
   *       // record that the happy face has been drawn
   *       this.isDrawn = true;
   *     },
   *     // color of the happy face (default is yellow)
   *     color: 'yellow',
   *     getColor: function() { return this.color },
   *     setColor: function(color) { this.color = color }
   *   }
   * });
   *
   * // draw a blue happy face
   * var hf = new HappyFace('blue');
   * hf.draw();
   * log(hf.isDrawn); // => true
   * log(hf.does(TFace)); // => true
   * log(HappyFace.does(TColoredCircle)); // => true
   * log(HappyFace.superclass === Doodle); // => true
   *
   * @name Class.define
   * @static
   * @function
   *
   * @throws {Trait.TraitError} Throws an error if the trait arguments are
   *    invalid, there is an unresolved conflict, or there is an unfullfilled
   *    trait requirement.
   * @return {Function} Constructor for the newly created class.
   * @param {Object} o The class configuration object.
   * @param {Function} [o.superclass] Superclass from which this class
   *    inherits.  Default superclass is the global object Class.
   * @param {Trait|Trait[]} [o.uses] A list of traits that will be composed
   *    into this class.  This happens by first constructing a new anonymous
   *    trait and then adding each of that anonymous trait's exported methods
   *    into the class prototype.  Trait methods are not copied, however, if
   *    there is a method defined at the class level with the same name
   *    (because class level methods override trait level methods).  Unlike
   *    normal trait definition, all trait requirements must be fullfilled at
   *    class definition time, either by one of the composed traits, a class
   *    method, or a superclass method.  See the documentation for o.uses in
   *    {@link Trait} for full details on how to specify this argument.
   * @param {Object} [o.members] Public instance members to be copied into this
   *    class's prototype.
   */
  Class.define = function(o) {
    if (!o.superclass) {
      o.superclass = Class;
    }

    var _super = o.superclass.prototype,
        prop = o.members || {};

    // Instantiate a base class (but only create the instance,
    // don't run the init constructor)
    initializing = true;
    var prototype = new o.superclass();
    initializing = false;

    // Merge in traits (if any) to list of properties (prop)
    if (o.uses) {
      var trait = Trait.define({
        uses: o.uses,
        _klass_prototype: prototype,
        _klass_properties: prop
      });

      // Augment the class with the newly created trait
      for (var method_name in trait._exports) {
        if (!trait._exports.hasOwnProperty(method_name)) continue;

        // Class methods (but not superclass methods) have precedence over
        // trait methods, so don't import the method if this class already
        // has a method with the same name.
        if (prop.hasOwnProperty(method_name)) {
          // If the trait method is being overriden by the class, the
          // corresponding class slot must be callable.
          if (typeof prop[method_name] != "function") {
            throw new Trait.TraitError(method_name +
                " overrides trait method and must be a function.");
          }
        } else {
          prop[method_name] = trait._exports[method_name];
        }
      }
    }

    // Copy the properties over to the new prototype
    for (var name in prop) {
      if (!prop.hasOwnProperty(name)) continue;

      // Check if we're overwriting an existing function. If we are,
      // create a closure to point this._super to the overriden method
      // in the superclass only while the execution is running.  We
      // preserve the original value of _super and restore it so that if
      // a method x calls another method y this._super will work as
      // expected in method x after calling y.
      prototype[name] = typeof prop[name] == "function" &&
        typeof _super[name] == "function" && fnTest.test(prop[name]) ?
        (function(name, fn){
          return function() {
            var tmp = this._super;

            // Add a new ._super() method that is the same method
            // but on the superclass.  Naming it _super because
            // super (no underscore) is a JS reserved word.
            this._super = _super[name];

            // The method only need to be bound temporarily, so we
            // remove it when we're done executing
            var ret = fn.apply(this, arguments);
            this._super = tmp;

            return ret;
          };
        })(name, prop[name]) :
        prop[name];
    }

    // The dummy class constructor
    var klass = function() {
      // All construction is actually done in the init method
      if ( !initializing && this.init ) {
        this.init.apply(this, arguments);
      }
    }

    // Populate our constructed prototype object
    klass.prototype = prototype;

    // Enforce the constructor to be what we expect
    klass.constructor = klass;

    // Add a static reference to the superclass
    klass.superclass = o.superclass;

    // Add the type string -ben
    klass.prototype.type = o.type;

    // Alias the does operator from the implict anonymous trait as an
    // instance and static method of the class.  Return false/empty if
    // no traits are associated with this class (or any superclass).
    klass.does = function(trait_ref) {
      if (!trait) {
        if (klass.superclass.does)
          return klass.superclass.does(trait_ref);
        if (trait_ref)
          return false;
        return [];
      }

      var used_traits = trait.does(trait_ref);
      if (klass.superclass.does) {
        if (trait_ref)
          return used_traits || klass.superclass.does(trait_ref);
        // merge inherited traits and those used in this class (note:
        // concat([]) is an idiom for copying an array).  It would be quicker
        // to sort these arrays and then merge algorithmically, but JS
        // doesn't appear to sort references consistently using the
        // Array.sort method...it sorts by lexicographical order of source
        // code string in Gecko, which is definitely not what we want...
        var inherited_traits = klass.superclass.does(trait_ref).concat([]);
        for (var i = used_traits.length-1; i >= 0; i--) {
          if (inherited_traits.indexOf(used_traits[i]) === -1)
            inherited_traits.push(used_traits[i]);
        }
        return inherited_traits;
      }

      return used_traits;
    };
    if (!klass.prototype.hasOwnProperty("does")) {
      klass.prototype.does = klass.does;
    }

    return klass;
  };

  ////////////////////////////////////////////////////////////////////////////
  //
  // Traits
  //
  ////////////////////////////////////////////////////////////////////////////

  // Some helper functions used by Trait

  // Is o an array?
  function isArray(o) {
    return Object.prototype.toString.apply(o) === '[object Array]';
  }

  // Make o an array if it isn't one already
  function makeArray(o) {
    if (o)
      return isArray(o) ? o : [o];
    return [];
  }

  // Convert ['a', 'b'] to { 'a':true, 'b':true }
  // Also converts 'a' to { 'a':true }
  function stringArrayToHash(a) {
    if (!a) return {};
    var ret = {};
    if (!isArray(a)) {
      ret[a] = true;
      return ret;
    }
    for (var i = a.length-1; i >=0; i--)
      ret[a[i]] = true;
    return ret;
  }

  // Merge sender's properties into receiver
  function merge(receiver, sender) {
    for (var i in sender) {
      if (!sender.hasOwnProperty(i)) continue;
      receiver[i] = sender[i];
    }
    return receiver;
  }

  this.Trait = Class.define({
    members: /** @lends Trait.prototype */ {
      /**
       * <p>
       * A trait is a group of pure methods that serves as a building block for
       * classes and is a primitive unit of code reuse. In this model, classes
       * are composed from a set of traits by specifying glue code that
       * connects the traits together and accesses the necessary state.  If you
       * are unfamiliar with the general object oriented programming notion of
       * a trait it would serve you well to check out the
       * <a href="http://code.google.com/p/jstraits/wiki/TraitSynopsis">synopsis
       * and examples</a> before reading the rest of this documentation.
       * </p>
       *
       * <p>
       * The constructor creates a new trait for use in other traits or classes.
       * The factory method {@link Trait.define} is the preferred way to
       * instantiate new traits, as opposed to calling 'new Trait(...)'
       * directly.
       * </p>
       *
       * @example
       * var TColoredCircle = new Trait({
       *   uses: [TColor, TCircle.aliases({'drawOutline': 'draw'})],
       *   requires: 'fillWithColor',
       *   methods: {
       *     draw: function() {
       *       // draw a colored circle
       *       this.drawOutline();
       *       this.fillWithColor(this.getColor());
       *     }
       *   }
       * });
       *
       * @constructs
       * @see Trait.define
       * @throws {Trait.TraitError} Throws an error if the trait definition
       *    arguments are invalid, the trait definition is inconsistent, or
       *    there is an unresolved conflict.
       * @param {Object} o The trait configuration object.
       * @param {Trait|Trait[]} [o.uses] Other trait(s) that will be composed
       *    into this new trait.  Note that trait composition is both
       *    associative and commutative, so if specifying more than one trait
       *    the order does not matter.  To alias or exclude methods from
       *    subtraits as they are composed, call {@link Trait#aliases} or
       *    {@link Trait#excludes}.  Calls to these functions may be chained.
       *    Passing a trait into o.uses causes the methods from that trait
       *    (plus any aliases and modulo any excludes) to be added to the new
       *    trait.  If any of these exported method names conflict with another
       *    trait specified in o.uses, the conflict must be resolved (unless
       *    the conflicting method names point to the exact same function).
       *    Conflicts may be resolved by either 1) overriding the method in
       *    o.methods 2) overriding the method at the class level (if this
       *    trait is being defined as part of a class) or 3) excluding all but
       *    one of the conflicting methods.
       * @param {String|String[]} [o.requires] A list of method names that must 
       *    be implemneted before this trait can function properly.  A trait
       *    may have open requirements, but all its requirements must be
       *    fulfilled when it is composed into a class.  Requirements can be
       *    satisfied by other traits or class methods.
       * @param {Object} [o.methods] A dictionary of methods that this trait
       *    exports (in addition to those composed in o.uses).  Methods may
       *    access instance methods, but should not directly access instance
       *    variables.  References to instance methods that are not deinfed in
       *    this trait or a composed subtrait should have their method names
       *    placed in the o.requires parameter.
       */
      init: function(o) {
        // Normalize arguments
        this._subtraits = makeArray(o.uses);
        this._requires = stringArrayToHash(o.requires);
        this._exports = o.methods ? o.methods : {};

        // The set of exported methods and required methods of a trait must
        // be disjoint
        var method_name;
        for (method_name in this._requires) {
          if (!this._requires.hasOwnProperty(method_name)) continue;
          if (this._exports.hasOwnProperty(method_name)) {
            throw new Trait.TraitError("Trait cannot require and provide " +
              "the same method " + method_name);
          }
        }

        // Compose subtraits with this trait
        var subtrait, exports, i, excludes = {};
        for (i = this._subtraits.length-1; i >= 0; i--) {
          subtrait = this._subtraits[i];

          // Merge aliases into list of exports.
          exports = merge({}, subtrait._exports);
          if (subtrait._aliases)
            merge(exports, subtrait._aliases);

          // Note if any methods were excluded from this subtrait. If they
          // were a method with the same name will need to be provided in this
          // trait. We'll check this after composition finishes. Also,
          // remove method from list of exports.
          if (subtrait._excludes) {
            for (method_name in subtrait._excludes) {
              if (!subtrait._excludes.hasOwnProperty(method_name)) continue;
              delete exports[method_name];
              merge(excludes, subtrait._excludes);
            }
          }

          // Compose the subtrait's exported methods into this trait
          for (method_name in exports) { // each exported method
            if (!exports.hasOwnProperty(method_name)) continue;
            // If this method name is overriden at the class level don't
            // compose it.  Overriding at the class level also resolves any
            // potential conflicts for this method name, so we don't have to
            // check for that.  Superclass methods, however, should be
            // overriden by trait methods, so we won't check the classes
            // prototype here.
            if (o._klass_prototype &&
                o._klass_properties.hasOwnProperty(method_name) &&
                typeof o._klass_properties[method_name] == "function")
            {
              // overriding at class level, do nothing
              continue;
            }

            // If we've already exported a method with name method_name and
            // that exported method is from another subtrait then we have
            // a TraitConflict.  Otherwise, if the already exported method is
            // defined in this trait, it overrides methods with the same name
            // from any subtrait (and hence we don't assign the subtrait's
            // method here). An error is not thrown if the subtrait's method
            // is the same as the one that already exists.
            if (this._exports.hasOwnProperty(method_name)) {
              if ((!o.methods || !o.methods.hasOwnProperty(method_name)) &&
                 this._exports[method_name] != exports[method_name])
              {
                throw new Trait.TraitError("Multiple subtraits provide " +
                   "method " + method_name + " creating a conflict. " +
                   "Exclude all but one of the methods or override the " +
                   "method in the trait/class to resolve.");
              }
              continue;
            }

            // No overrides and no conflicts so merge the subtrait's exported
            // method into this trait.
            this._exports[method_name] = exports[method_name];
          }

          // Compose the subtrait's required methods into this trait
          for (method_name in subtrait._requires) {
            if (!subtrait._requires.hasOwnProperty(method_name)) continue;
            if (!this._exports.hasOwnProperty(method_name)) {
              this._requires[method_name] = true;
            }
          }

          // Clear out alias and exclude data (which only makes sense in
          // the context of a 'use' clause).
          delete subtrait._aliases
          delete subtrait._excludes
        }

        // Make sure that excluded methods have been overriden either by
        // another trait or class
        for (method_name in excludes) {
          if (!excludes.hasOwnProperty(method_name)) continue;
          if (!this._exports.hasOwnProperty(method_name) &&
              (!o._klass_prototype ||
               (typeof o._klass_prototype[method_name] != "function" &&
                typeof o._klass_properties[method_name] != "function"
               )))
          {
            throw new Trait.TraitError("Excluded method " + method_name +
                " must be provided by another class or trait.");
          }
        }

        // Prune any requirements that are fullfilled by the composed trait.
        // If this trait is being defined in a class, make sure that all other
        // requirements are fullfilled by the class.
        for (method_name in this._requires) {
          if (!this._requires.hasOwnProperty(method_name)) continue;
          if (this._exports.hasOwnProperty(method_name)) {
            delete this._requires[method_name];
          } else if (o._klass_prototype &&
                     (typeof o._klass_prototype[method_name] != "function" &&
                      typeof o._klass_properties[method_name] != "function"
                     ))

          {
            throw new Trait.TraitError("Trait requirement " + method_name +
                " not fullfilled by class.");
          }
        }
      },

      /**
       * Alias a method to a different name during trait composition.  Aliases
       * should only be used in a 'uses' clause inside a class or trait
       * definition.  It may be chained with {@link Trait#excludes}.  Aliasing
       * a method causes it to be copied under the new alias name in addition
       * to the original method name.  Multiple aliases may be made to the
       * same function.  Aliases are treated exactly like normal method names
       * during trait composition.
       * 
       * @throws {Trait.TraitError} Throws a TraitError if attempting to alias a
       *     non-existent function, create an alias with the same name as a
       *     natively exported method, or create an alias with the same name
       *     as one of the required method names.
       * @return {Trait} this
       * @param {Object} o A String to String mapping of alias names to exported
       *     method names.
       */
      aliases: function(o) {
        this._aliases = this._aliases || {};
        // Check that alias targets exist, aliases don't override existing
        // methods, and aliases/requirements are disjoint
        for (var alias in o) {
          if (!o.hasOwnProperty(alias)) continue;
          if (!this._exports.hasOwnProperty(o[alias]))
            throw new Trait.TraitError("can't alias " + alias
                + " to " + o[alias] +
                " because trait doesn't have that method");
          if (this._exports.hasOwnProperty(alias))
            throw new Trait.TraitError("can't create an alias with name " +
                alias + " because trait natively exports that method");
          if (this._requires.hasOwnProperty(alias))
            throw new Trait.TraitError("can't create an alias with name " +
                alias + " because trait requires method with same name");
          this._aliases[alias] = this._exports[o[alias]];
        }
        return this;
      },

      /**
       * Exclude a method during trait composition.  Excludes should only be
       * used in a 'uses' clause inside a class or trait definition.  It may be
       * chained with {@link Trait#aliases}.  Excluding a method causes it to
       * not be copied into the containing class or trait as it normally would.
       * If a method is excluded a method with the same name must be provided,
       * either by another trait or a class method.
       * 
       * @throws {Trait.TraitError} Throws a TraitError if attempting to exclude
       *     a method that is not exported by this trait.
       * @returns {Trait} this
       * @param {String|String[]} a Method(s) to exclude during trait
       *     composition.
       */
      excludes: function(a) {
        this._excludes = this._excludes || {};
        a = makeArray(a);
        // Check that excluded methods exist
        for (var i = a.length-1; i >=0; i--) {
          if (!this._exports.hasOwnProperty(a[i])) {
            throw new Trait.TraitError("can't exclude method " + a[i] +
                " because no such method exists in trait");
          }
          this._excludes[a[i]] = true;
        }
        return this;
      },

      /**
       * Inspect all traits used by this trait.  Note that this trait is
       * included in the list of what this trait 'does'. If no argument is
       * passed, an array of all traits is returned.  If a trait is passed, a
       * boolean is returned indicating if the specified trait is one of the
       * composed traits.  This method differs from {@link Trait#subtraits} in
       * that subtraits only checks for traits specified in the use clause,
       * while this method recursively checks to see if any of the subtraits'
       * subraits match, and so on.
       *
       * @return {Trait[]|Boolean} List of all traits composed into this trait
       *     or a boolean indicating if a particular trait was used.
       * @param {Trait} [trait_ref] Trait to check for inclusion in the list
       *     of composed traits.
       */
      does: function(trait_ref) {
        // Computing the list of implemented traits can be a little expensive
        // so lazily generate it on the first invocation of does
        if (!this._does) {
          // This trait does itself, its subtraits, and . . .
          this._does = [this].concat(this._subtraits);
          // . . . its subtraits' subtratis, etc..
          var i, j, subsub;
          for (i = this._subtraits.length-1; i >= 0; i--) {
            subsub = this._subtraits[i].does();
            // Since the same trait can be acquired from multiple sources,
            // need to check traits for uniqueness before adding to _does.
            // This would be more efficient with a dictionary but JS objects
            // don't support pointer/reference keys, and I have a feeling that
            // a hand rolled dictionary implementation is going to be slooow.
            for (j = subsub.length-1; j >= 0; j--) {
              if (this._does.indexOf(subsub[j]) === -1)
                this._does.push(subsub[j]);
            }
          }
        }

        if (trait_ref)
          return this._does.indexOf(trait_ref) >= 0;
        return this._does;
      },

      /**
       * Inspect method names required by this trait.  If no arguments are
       * passed, an object with keys representing all the required methods is 
       * returned.  If a string argument is given, requires returns a boolean
       * indicating if this trait requires a method with that name.
       *
       * @return {Object|Boolean} Object keyed by required method names or
       *     boolean indicating if a particular method name is required.
       * @param {String} method_name Method name to check if in required method
       *     name list.
       */
      requires: function(method_name) {
        if (method_name)
          return this._requires.hasOwnProperty(method_name) &&
            this._requires[method_name];
        return this._requires;
      },

      /**
       * Inspect subtraits used by this trait. Note that only immediate
       * subtraits are dealt with here (i.e. those passed in the 'uses'
       * clause). To recursively check if a trait uses another trait see
       * {@link Trait#does}.  If no argument is passed, an array of all
       * subtraits is returned.  If a trait is passed, a boolean is returned
       * indicating if the specified trait is one of the subtraits.
       *
       * @return {Trait[]|Boolean} List of all subtraits or boolean indicating
       *     if a particular subtrait was used.
       * @param {Trait} [trait_ref] Trait to check for inclusion in the list
       *     of subtraits.
       */
      subtraits: function(trait_ref) {
        if (trait_ref)
          return this._subtraits.indexOf(trait_ref) >= 0;
        return this._subtraits;
      },

      /**
       * Inspect methods exported by this trait.  If no arguments are passed,
       * an object mapping each method name exported by this trait to its
       * associated function is returned.  If a string argument is given,
       * methods checks if this trait exports a method with that name.  If so
       * it returns the associated function, otherwise it returns undefined.
       *
       * @return {Object|Function} Mapping of method names to functions, a
       *     specific function, or undefined.
       * @param {String} [method_name] Name of the method to look up in this
       *     trait's method export list.
       */
      methods: function(method_name) {
        if (method_name)
          return this._exports.hasOwnProperty(method_name) &&
            this._exports[method_name];
        return this._exports;
      }
    }
  });

  /**
   * Factory method to create new traits.  Arguments are the same as those
   * passed to the {@link Trait} constructor.  This static method is the
   * preferred way to create new traits.
   *
   * @example
   * var TColoredCircle = Trait.define({
   *   uses: [TColor, TCircle.aliases({'drawOutline': 'draw'})],
   *   requires: 'fillWithColor',
   *   methods: {
   *     draw: function() {
   *       // draw a colored circle
   *       this.drawOutline();
   *       this.fillWithColor(this.getColor());
   *     }
   *   }
   * });
   *
   * @static
   * @memberOf Trait
   * @name define
   * @function
   * @see Trait
   * @return {Trait}
   * @throws {Trait.TraitError}
   * @param {Object} o
   * @param {Trait|Trait[]} [o.uses]
   * @param {String|String[]} [o.requires]
   * @param {Object} [o.methods]
   */
  Trait.define = function(o) {
    return new Trait(o);
  };

  Trait.TraitError = Class.define({
    superclass: Error,
    members: /** @lends Trait.TraitError.prototype */ {
      /**
       * Generic error thrown for any trait related exceptions.
       *
       * @constructs
       * @augments Error
       * @param {String} msg The message to show when printing out the string.
       */                                                     
      init: function(msg) {
        this.name = "TraitError";
        this.message = msg;
      }
    }
  });
})();
