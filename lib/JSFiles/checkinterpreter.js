function nobaseTest() {
   for (let key in this) {
      if (key == 'nobaseTest') continue;
      console.log(key,typeof this[key]);
      if (typeof this[key] == 'object') {
	 for (let key1 in this[key]) {
	    console.log(key + "." + key1,typeof this[key][key1]);
	  }
       }
    }
}


let nobaseDone = { };
let nobaseODone = [ ];

function nobaseDump(obj,pfx)
{
   if (pfx == "") {
    }

   for (let key in obj) {
      if (key == 'nobaseTest' || key == 'nobaseDump' || key == 'nobaseDone' ||
	     key == 'nobaseODone') continue;
      let nkey = obj[key];
      if (nkey != null && typeof nkey == 'object') {
	 let idx = nobaseODone.indexOf(nkey);
	 if (idx >= 0) continue;
       }
      else if (nobaseDone[nkey] == true) continue;
      if (typeof obj[key] == 'string' && key != '_eval') {
	 console.log(pfx + key,typeof obj[key],encodeURI(obj[key]));
       }
      else if (Array.isArray(obj[key])) {
	 console.log(pfx + key,"Array",obj[key].length);
       }
      else if (typeof obj[key] == 'undefined') ;
      else {
	 console.log(pfx + key,typeof obj[key]);
       }
    }

   for (let key in obj) {
      if (key == 'nobaseTest' || key == 'nobaseDump') continue;
      let nkey = obj[key];
      if (typeof nkey != 'object') {
	 if (nobaseDone[nkey] == true) continue;
	 nobaseDone[nkey] = true;
       }
      else {
	 let idx = nobaseODone.indexOf(nkey);
	 if (idx >= 0) continue;
	 nobaseODone.push(nkey);
       }

      if (nkey === obj) continue;
      if (typeof nkey == 'object') {
	 if (!Array.isArray(nkey)) {
	    nobaseDump(nkey,pfx + key + ".");
	  }
       }
      else if (typeof nkey == 'function') {
	 nobaseDump(obj[key],pfx + key + ".");
       }
    }

}

function nobaseDumpReq(req)
{
   let x = require(req);
   let nm = "REQUIRE$$$_" + req;
   console.log(nm,"object");
   nobaseDump(x,nm + ".");
}


function dumpBuiltins()
{
   console.log("NaN",typeof NaN);
   console.log("eval",typeof eval);
   console.log("parseInt",typeof parseInt);
   console.log("Infinity",typeof Infinity);
   console.log("parseFloat",typeof parseFloat);
   console.log("isNaN",typeof isNaN);
   console.log("isFinite",typeof isFinite);
   console.log("decodeURI",typeof decodeURI);
   console.log("decodeURIComponent",typeof decodeURIComponent);
   console.log("encodeURI",typeof encodeURI);
   console.log("encodeURIComponent",typeof encodeURIComponent);
   console.log("Object",typeof Object);
   console.log("Function",typeof Function);
   console.log("Array",typeof Array);
   console.log("String",typeof String);
   console.log("Boolean",typeof Boolean);
   console.log("Number",typeof Number);
   console.log("Date",typeof Date);
   console.log("RegExp",typeof RegExp);
   console.log("Symbol",typeof Symbol);
   console.log("BigInt",typeof BigInt);
   console.log("Int8Array",typeof Int8Array);
   console.log("Uint8Array",typeof Uint8Array);
   console.log("Uint8ClampedArray",typeof Uint8ClampedArray);
   console.log("Int16Array",typeof Int16Array);
   console.log("Uint16Array",typeof Uint16Array);
   console.log("Int32Array",typeof Int32Array);
   console.log("Uint32Array",typeof Uint32Array);
   console.log("Float32Array",typeof Float32Array);                     // todo
   console.log("Float64Array",typeof Float64Array);                     // todo
   console.log("BigInt64Array",typeof BigInt64Array);                   // todo
   console.log("BigUint64Array",typeof BigUint64Array);                 // todo
   console.log("Map",typeof Map);
   console.log("Set",typeof Set);
   console.log("WeakMap",typeof WeakMap);
   console.log("WeakSet",typeof WeakSet);
   console.log("ArrayBuffer",typeof ArrayBuffer);                       // todo
   console.log("SharedArrayBuffer",typeof SharedArrayBuffer);           // todo
   console.log("Atomics",typeof Atomics);                               // todo
   console.log("DataView",typeof DataView);                             // todo
   console.log("JSON",typeof JSON);                                     // todo
   console.log("Promise",typeof Promise);                               // todo
   console.log("Reflect",typeof Reflect);                               // todo
   console.log("Proxy",typeof Proxy);                                   // todo
   console.log("Intl",typeof Intl);                                     // todo plus subclasses
   console.log("WebAssembly",typeof WebAssembly);                       // todo plus subclasses

   console.log("Error",typeof Error);
   console.log("EvalError",typeof EvalError);
   console.log("RangeError",typeof RangeError);
   console.log("ReferenceError",typeof ReferenceError);
   console.log("SyntaxError",typeof SyntaxError);
   console.log("TypeError",typeof TypeError);
   console.log("URIError",typeof URIError);
   console.log("Math",typeof Math);
   console.log("Object.constructor",typeof Object.constructor);
   console.log("Object.prototype",typeof Object.prototype);
   console.log("Object.assign",typeof Object.assign);
   console.log("Object.getPrototypeOf",typeof Object.getPrototypeOf);
   console.log("Object.getOwnPropertyDescriptor",typeof Object.getOwnPropertyDescriptor);
   console.log("Object.getOwnPropertyDescriptors",typeof Object.getOwnPropertyDescriptors);
   console.log("Object.getOwnPropertyNames",typeof Object.getOwnPropertyNames);
   console.log("Object.getOwnPropertySymbols",typeof Object.getOwnPropertySymbols);
   console.log("Object.getPrototypeOf",typeof Object.getPrototypeOf);
   console.log("Object.setPrototypeOf",typeof Object.setPrototypeOf);
   console.log("Object.create",typeof Object.create);
   console.log("Object.defineProperty",typeof Object.defineProperty);
   console.log("Object.defineProperties",typeof Object.defineProperties);
   console.log("Object.entries",typeof Object.entries);
   console.log("Object.fromEntries",typeof Object.fromEntries);
   console.log("Object.seal",typeof Object.seal);
   console.log("Object.freeze",typeof Object.freeze);
   console.log("Object.preventExtensions",typeof Object.preventExtensions);
   console.log("Object.is",typeof Object.is);
   console.log("Object.isSealed",typeof Object.isSealed);
   console.log("Object.isFrozen",typeof Object.isFrozen);
   console.log("Object.isExtensible",typeof Object.isExtensible);
   console.log("Object.keys",typeof Object.keys);
   console.log("Object.values",typeof Object.values);
   console.log("Object.prototype.constructor",typeof Object.prototype.constructor);
   console.log("Object.prototype.__proto__",typeof Object.prototype.__proto__);
   console.log("Object.prototype.__defineGetter__",typeof Object.prototype.__defineGetter__);
   console.log("Object.prototype.__defineSetter__",typeof Object.prototype.__defineSetter__);
   console.log("Object.prototype.__lookupGetter__",typeof Object.prototype.__lookupGetter__);
   console.log("Object.prototype.__lookupASetter__",typeof Object.prototype.__lookupSetter__);
   console.log("Object.prototype.toString",typeof Object.prototype.toString);
   console.log("Object.prototype.toLocaleString",typeof Object.prototype.toLocaleString);
   console.log("Object.prototype.valueOf",typeof Object.prototype.valueOf);
   console.log("Object.prototype.hasOwnProperty",typeof Object.prototype.hasOwnProperty);
   console.log("Object.prototype.isPrototypeOf",typeof Object.prototype.isPrototypeOf);
   console.log("Object.prototype.propertyIsEnumerable",typeof Object.prototype.propertyIsEnumerable);
   console.log("Function.constructor",typeof Function.constructor);
   console.log("Function.prototype",typeof Function.prototype);
   console.log("Function.length",typeof Function.length);
   console.log("Function.prototype.constructor",typeof Function.prototype.constructor);
   console.log("Function.prototype.toString",typeof Function.prototype.toString);
   console.log("Function.prototype.apply",typeof Function.prototype.apply);
   console.log("Function.prototype.call",typeof Function.prototype.call);
   console.log("Function.prototype.bind",typeof Function.prototype.bind);
   console.log("Function.call",typeof Function.call);
   console.log("Array.constructor",typeof Array.constructor);
   console.log("Array.prototype",typeof Array.prototype);
   console.log("Array.isArray",typeof Array.isArray);
   console.log("Array.from",typeof Array.from);
   console.log("Array.of",typeof Array.of);
   console.log("Array.prototype.constructor",typeof Array.prototype.constructor);
// console.log("Array.prototype.at",typeof Array.prototype.at);
   console.log("Array.prototype.toString",typeof Array.prototype.toString);
   console.log("Array.prototype.toLocaleString",typeof Array.prototype.toLocaleString);
   console.log("Array.prototype.concat",typeof Array.prototype.concat);
   console.log("Array.prototype.copyWithin",typeof Array.prototype.copyWithin);
   console.log("Array.prototype.join",typeof Array.prototype.join);
   console.log("Array.prototype.pop",typeof Array.prototype.pop);
   console.log("Array.prototype.push",typeof Array.prototype.push);
   console.log("Array.prototype.reverse",typeof Array.prototype.reverse);
   console.log("Array.prototype.shift",typeof Array.prototype.shift);
   console.log("Array.prototype.slice",typeof Array.prototype.slice);
   console.log("Array.prototype.sort",typeof Array.prototype.sort);
   console.log("Array.prototype.splice",typeof Array.prototype.splice);
   console.log("Array.prototype.indexOf",typeof Array.prototype.indexOf);
   console.log("Array.prototype.lastIndexOf",typeof Array.prototype.lastIndexOf);
   console.log("Array.prototype.some",typeof Array.prototype.some);
   console.log("Array.prototype.map",typeof Array.prototype.map);
   console.log("Array.prototype.filter",typeof Array.prototype.filter);
   console.log("Array.prototype.reduce",typeof Array.prototype.reduce);
   console.log("Array.prototype.reduceRight",typeof Array.prototype.reduceRight);
   console.log("Array.prototype.forEach",typeof Array.prototype.forEach);
   console.log("Array.prototype.unshift",typeof Array.prototype.unshift);
   console.log("Array.prototype.entries",typeof Array.prototype.entries);
   console.log("Array.prototype.every",typeof Array.prototype.every);
   console.log("Array.prototype.fill",typeof Array.prototype.fill);
   console.log("Array.prototype.filter",typeof Array.prototype.filter);
   console.log("Array.prototype.find",typeof Array.prototype.find);
   console.log("Array.prototype.findIndex",typeof Array.prototype.findIndex);
   console.log("Array.prototype.flat",typeof Array.prototype.flat);
   console.log("Array.prototype.flatMap",typeof Array.prototype.flatMap);
// console.log("Array.prototype.groupBy",typeof Array.prototype.groupBy);
// console.log("Array.prototype.groupByToMap",typeof Array.prototype.groupByToMap);
   console.log("Array.prototype.includes",typeof Array.prototype.includes );
   console.log("Array.prototype.keys",typeof Array.prototype.keys);
   console.log("Array.prototype.reverse",typeof Array.prototype.reverse);
   console.log("Array.prototype.values",typeof Array.prototype.values);
   console.log("Array.length",typeof Array.length);
   console.log("Array.prototype.length",typeof Array.prototype.length);
   console.log("String.constructor",typeof String.constructor);
   console.log("String.prototype",typeof String.prototype);
   console.log("String.fromCharCode",typeof String.fromCharCode);
   console.log("String.prototype.constructor",typeof String.prototype.constructor);
   console.log("String.prototype.toString",typeof String.prototype.toString);
   console.log("String.prototype.valueOf",typeof String.prototype.valueOf);
   console.log("String.prototype.charAt",typeof String.prototype.charAt);
   console.log("String.prototype.charCodeAt",typeof String.prototype.charCodeAt);
   console.log("String.prototype.concat",typeof String.prototype.concat);
   console.log("String.prototype.indexOf",typeof String.prototype.indexOf);
   console.log("String.prototype.lastIndexOf",typeof String.prototype.lastIndexOf);
   console.log("String.prototype.localeCompare",typeof String.prototype.localeCompare);
   console.log("String.prototype.match",typeof String.prototype.match);
   console.log("String.prototype.replace",typeof String.prototype.replace);
   console.log("String.prototype.search",typeof String.prototype.search);
   console.log("String.prototype.slice",typeof String.prototype.slice);
   console.log("String.prototype.split",typeof String.prototype.split);
   console.log("String.prototype.substr",typeof String.prototype.substr);
   console.log("String.prototype.substring",typeof String.prototype.substring);
   console.log("String.prototype.toLowerCase",typeof String.prototype.toLowerCase);
   console.log("String.prototype.toLocaleLowerCase",typeof String.prototype.toLocaleLowerCase);
   console.log("String.prototype.toUpperCase",typeof String.prototype.toUpperCase);
   console.log("String.prototype.toLocaleUpperCase",typeof String.prototype.toLocaleUpperCase);
   console.log("String.prototype.trim",typeof String.prototype.trim);
   console.log("String.length",typeof String.length);
   console.log("String.prototype.anchor",typeof String.prototype.anchor);
   console.log("String.prototype.big",typeof String.prototype.big);
   console.log("String.prototype.blink",typeof String.prototype.blink);
   console.log("String.prototype.bold",typeof String.prototype.bold);
   console.log("String.prototype.fixed",typeof String.prototype.fixed);
   console.log("String.prototype.fontcolor",typeof String.prototype.fontcolor);
   console.log("String.prototype.fontsize",typeof String.prototype.fontsize);
   console.log("String.prototype.italics",typeof String.prototype.italics);
   console.log("String.prototype.link",typeof String.prototype.link);
   console.log("String.prototype.small",typeof String.prototype.small);
   console.log("String.prototype.strike",typeof String.prototype.strike);
   console.log("String.prototype.sub",typeof String.prototype.sub);
   console.log("String.prototype.sup",typeof String.prototype.sup);
   console.log("Boolean.constructor",typeof Boolean.constructor);
   console.log("Boolean.prototype",typeof Boolean.prototype);
   console.log("Boolean.prototype.constructor",typeof Boolean.prototype.constructor);
   console.log("Boolean.prototype.toString",typeof Boolean.prototype.toString);
   console.log("Boolean.prototype.valueOf",typeof Boolean.prototype.valueOf);
   console.log("Number.constructor",typeof Number.constructor);
   console.log("Number.MAX_VALUE",typeof Number.MAX_VALUE);
   console.log("Number.MIN_VALUE",typeof Number.MIN_VALUE);
   console.log("Number.NaN",typeof Number.NaN);
   console.log("Number.NEGATIVE_INFINITY",typeof Number.NEGATIVE_INFINITY);
   console.log("Number.POSITIVE_INFINITY",typeof Number.POSITIVE_INFINITY);
   console.log("Number.EPSILON",typeof Number.EPSILON);
   console.log("Number.MAX_SAFE_INTEGER",typeof Number.MAX_SAFE_INTEGER);
   console.log("Number.MIN_SAFE_INTEGER",typeof Number.MIN_SAFE_INTEGER);
   console.log("Number.isNaN",typeof Number.isNaN);
   console.log("Number.isFinite",typeof Number.isFinite);
   console.log("Number.isInteger",typeof Number.isInteger);
   console.log("Number.isSafeInteger",typeof Number.isSafeInteger);
   console.log("Number.parseFloat",typeof Number.parseFloat);
   console.log("Number.parseInt",typeof Number.parseInt);
   console.log("Number.prototype",typeof Number.prototype);
   console.log("Number.prototype.constructor",typeof Number.prototype.constructor);
   console.log("Number.prototype.toString",typeof Number.prototype.toString);
   console.log("Number.prototype.toLocaleString",typeof Number.prototype.toLocaleString);
   console.log("Number.prototype.valueOf",typeof Number.prototype.valueOf);
   console.log("Number.prototype.toFixed",typeof Number.prototype.toFixed);
   console.log("Number.prototype.toExponential",typeof Number.prototype.toExponential);
   console.log("Number.prototype.toPrecision",typeof Number.prototype.toPrecision);
   console.log("Math.E",typeof Math.E);
   console.log("Math.LN10",typeof Math.LN10);
   console.log("Math.LN2",typeof Math.LN2);
   console.log("Math.LOG2E",typeof Math.LOG2E);
   console.log("Math.LOG10E",typeof Math.LOG10E);
   console.log("Math.PI",typeof Math.PI);
   console.log("Math.SQRT1_2",typeof Math.SQRT1_2);
   console.log("Math.SQRT2",typeof Math.SQRT2);
   console.log("Math.abs",typeof Math.abs);
   console.log("Math.acos",typeof Math.acos);
   console.log("Math.asin",typeof Math.asin);
   console.log("Math.atan",typeof Math.atan);
   console.log("Math.atan2",typeof Math.atan2);
   console.log("Math.ceil",typeof Math.ceil);
   console.log("Math.cos",typeof Math.cos);
   console.log("Math.exp",typeof Math.exp);
   console.log("Math.floor",typeof Math.floor);
   console.log("Math.log",typeof Math.log);
   console.log("Math.max",typeof Math.max);
   console.log("Math.min",typeof Math.min);
   console.log("Math.pow",typeof Math.pow);
   console.log("Math.random",typeof Math.random);
   console.log("Math.round",typeof Math.round);
   console.log("Math.sin",typeof Math.sin);
   console.log("Math.sqrt",typeof Math.sqrt);
   console.log("Math.tan",typeof Math.tan);
   console.log("Date.constructor",typeof Date.constructor);
   console.log("Date.prototype.getDate",typeof Date.prototype.getDate);
   console.log("Date.prototype.getDay",typeof Date.prototype.getDay);
   console.log("Date.prototype.getFullYear",typeof Date.prototype.getFullYear);
   console.log("Date.prototype.getHours",typeof Date.prototype.getHours);
   console.log("Date.prototype.getMilliseconds",typeof Date.prototype.getMilliseconds);
   console.log("Date.prototype.getMinutes",typeof Date.prototype.getMinutes);
   console.log("Date.prototype.getMonth",typeof Date.prototype.getMonth);
   console.log("Date.prototype.getSeconds",typeof Date.prototype.getSeconds);
   console.log("Date.prototype.getTime",typeof Date.prototype.getTime);
   console.log("Date.prototype.getTimezoneOffset",typeof Date.prototype.getTimezoneOffset);
   console.log("Date.prototype.getUTCDate",typeof Date.prototype.getUTCDate);
   console.log("Date.prototype.getUTCDay",typeof Date.prototype.getUTCDay);
   console.log("Date.prototype.getUTCFullYear",typeof Date.prototype.getUTCFullYear);
   console.log("Date.prototype.getUTCHours",typeof Date.prototype.getUTCHours);
   console.log("Date.prototype.getUTCMilliseconds",typeof Date.prototype.getUTCMilliseconds);
   console.log("Date.prototype.getUTCMinutes",typeof Date.prototype.getUTCMinutes);
   console.log("Date.prototype.getUTCMonth",typeof Date.prototype.getUTCMonth);
   console.log("Date.prototype.getUTCSeconds",typeof Date.prototype.getUTCSeconds);
   console.log("Date.prototype.getYear",typeof Date.prototype.getYear);
   console.log("Date.prototype.setDate",typeof Date.prototype.setDate);
   console.log("Date.prototype.setFullYear",typeof Date.prototype.setFullYear);
   console.log("Date.prototype.setHours",typeof Date.prototype.setHours);
   console.log("Date.prototype.setMilliseconds",typeof Date.prototype.setMilliseconds);
   console.log("Date.prototype.setMinutes",typeof Date.prototype.setMinutes);
   console.log("Date.prototype.setMonth",typeof Date.prototype.setMonth);
   console.log("Date.prototype.setSeconds",typeof Date.prototype.setSeconds);
   console.log("Date.prototype.setTime",typeof Date.prototype.setTime);
   console.log("Date.prototype.setUTCDate",typeof Date.prototype.setUTCDate);
   console.log("Date.prototype.setUTCFullYear",typeof Date.prototype.setUTCFullYear);
   console.log("Date.prototype.setUTCHours",typeof Date.prototype.setUTCHours);
   console.log("Date.prototype.setUTCMilliseconds",typeof Date.prototype.setUTCMilliseconds);
   console.log("Date.prototype.setUTCMinutes",typeof Date.prototype.setUTCMinutes);
   console.log("Date.prototype.setUTCMonth",typeof Date.prototype.setUTCMonth);
   console.log("Date.prototype.setUTCSeconds",typeof Date.prototype.setUTCSeconds);
   console.log("Date.prototype.setYear",typeof Date.prototype.setYear);
   console.log("Date.prototype.toDateString",typeof Date.prototype.toDateString);
   console.log("Date.prototype.toGMTString",typeof Date.prototype.toGMTString);
   console.log("Date.prototype.toLocaleDateString",typeof Date.prototype.toLocaleDateString);
// console.log("Date.prototype.toLocaleFormat",typeof Date.prototype.toLocaleFormat);
   console.log("Date.prototype.toLocaleString",typeof Date.prototype.toLocaleString);
   console.log("Date.prototype.toLocaleTimeString",typeof Date.prototype.toLocaleTimeString);
   console.log("Date.prototype.toString",typeof Date.prototype.toString);
   console.log("Date.prototype.toTimeString",typeof Date.prototype.toTimeString);
   console.log("Date.prototype.toUTCString",typeof Date.prototype.toUTCString);
   console.log("Date.prototype.valueOf",typeof Date.prototype.valueOf);
   console.log("Date.parse",typeof Date.parse);
   console.log("Date.UTC",typeof Date.UTC);
   console.log("RegExp.constructor",typeof RegExp.constructor);
   console.log("RegExp.prototype.exec",typeof RegExp.prototype.exec);
   console.log("RegExp.prototype.test",typeof RegExp.prototype.test);
   console.log("RegExp.prototype.toString",typeof RegExp.prototype.toString);
   console.log("Symbol.constructor",typeof Symbol.constructor);
   console.log("Symbol.asyncIterator",typeof Symbol.asyncIterator);
   console.log("Symbol.hasInstance",typeof Symbol.hasInstance);
   console.log("Symbol.isConcatSpreadable",typeof Symbol.isConcatSpreadable);
   console.log("Symbol.iterator",typeof Symbol.iterator);
   console.log("Symbol.match",typeof Symbol.match);
   console.log("Symbol.matchAll",typeof Symbol.matchAll);
   console.log("Symbol.replace",typeof Symbol.replace);
   console.log("Symbol.search",typeof Symbol.search);
   console.log("Symbol.split",typeof Symbol.split);
   console.log("Symbol.species",typeof Symbol.species);
   console.log("Symbol.toPrimitive",typeof Symbol.toPrimitive);
   console.log("Symbol.toStringTag",typeof Symbol.toStringTag);
   console.log("Symbol.unscopables",typeof Symbol.unscopables);
   console.log("Symbol.for",typeof Symbol.for);
   console.log("Symbol.keyFor",typeof Symbol.keyFor);
   console.log("Symbol.prototype.toString",typeof Symbol.prototype.toString);
   console.log("Symbol.prototype.valueOf",typeof Symbol.prototype.valueOf);
   console.log("Symbol.prototype",typeof Symbol.prototype);
   console.log("Error.constructor",typeof Error.constructor);
   console.log("Error.captureStackTrace",typeof Error.captureStackTrace);
   console.log("Error.prototype.message",typeof Error.prototype.message);
   console.log("Error.prototype.name",typeof Error.prototype.name);
// console.log("Error.prototype.cause",typeof Error.prototype.cause);
// console.log("Error.prototype.fileName",typeof Error.prototype.fileName);
// console.log("Error.prototype.lineNumber",typeof Error.prototype.lineNumber);
// console.log("Error.prototype.columnNumber",typeof Error.prototype.columnNumber);
// console.log("Error.prototype.stack",typeof Error.prototype.stack);
   console.log("Error.prototype.toString",typeof Error.prototype.toString);
   console.log("BigInt.constructor",typeof BigInt.constructor);
   console.log("BigInt.asIntN",typeof BigInt.asIntN);
   console.log("BigInt.asUiintN",typeof BigInt.asUintN);
   console.log("BigInt.prototype.toLocaleString",typeof BigInt.prototype.toLocaleString);
   console.log("BigInt.prototype.toString",typeof BigInt.prototype.toString);
   console.log("BigInt.prototype.valueOf",typeof BigInt.prototype.valueOf);
   console.log("Int8Array.BYTES_PER_ELEMENT",typeof Int8Array.BYTES_PER_ELEMENT);
   console.log("Int8Array.name",typeof Int8Array.name);
   console.log("Int8Array.from",typeof Int8Array.from);
   console.log("Int8Array.of",typeof Int8Array.of);
   console.log("Int8Array.prototype.copyWithin",typeof Int8Array.prototype.copyWithin);
   console.log("Int8Array.prototype.entries",typeof Int8Array.prototype.entries);
   console.log("Int8Array.prototype.every",typeof Int8Array.prototype.every);
   console.log("Int8Array.prototype.fill",typeof Int8Array.prototype.fill);
   console.log("Int8Array.prototype.filter",typeof Int8Array.prototype.filter);
   console.log("Int8Array.prototype.find",typeof Int8Array.prototype.find);
   console.log("Int8Array.prototype.findIndex",typeof Int8Array.prototype.findIndex);
   console.log("Int8Array.prototype.forEach",typeof Int8Array.prototype.forEach);
   console.log("Int8Array.prototype.includes",typeof Int8Array.prototype.includes);
   console.log("Int8Array.prototype.indexOf",typeof Int8Array.prototype.indexOf);
   console.log("Int8Array.prototype.join",typeof Int8Array.prototype.join);
   console.log("Int8Array.prototype.keys",typeof Int8Array.prototype.keys);
   console.log("Int8Array.prototype.lastIndexOf",typeof Int8Array.prototype.lastIndexOf);
   console.log("Int8Array.prototype.map",typeof Int8Array.prototype.map);
   console.log("Int8Array.prototype.reduce",typeof Int8Array.prototype.reduce);
   console.log("Int8Array.prototype.reduceRight",typeof Int8Array.prototype.reduceRight);
   console.log("Int8Array.prototype.reverse",typeof Int8Array.prototype.reverse);
   console.log("Int8Array.prototype.set",typeof Int8Array.prototype.set);
   console.log("Int8Array.prototype.slice",typeof Int8Array.prototype.slice);
   console.log("Int8Array.prototype.some",typeof Int8Array.prototype.some);
   console.log("Int8Array.prototype.sort",typeof Int8Array.prototype.sort);
   console.log("Int8Array.prototype.subarray",typeof Int8Array.prototype.subarray);
   console.log("Int8Array.prototype.values",typeof Int8Array.prototype.values);
   console.log("Int8Array.prototype.toLocaleString",typeof Int8Array.prototype.toLocaleString);
   console.log("Int8Array.prototype.toString",typeof Int8Array.prototype.toString);
   console.log("Uint8Array.BYTES_PER_ELEMENT",typeof Uint8Array.BYTES_PER_ELEMENT);
   console.log("Uint8Array.name",typeof Uint8Array.name);
   console.log("Uint8Array.from",typeof Uint8Array.from);
   console.log("Uint8Array.of",typeof Uint8Array.of);
   console.log("Uint8Array.prototype.copyWithin",typeof Uint8Array.prototype.copyWithin);
   console.log("Uint8Array.prototype.entries",typeof Uint8Array.prototype.entries);
   console.log("Uint8Array.prototype.every",typeof Uint8Array.prototype.every);
   console.log("Uint8Array.prototype.fill",typeof Uint8Array.prototype.fill);
   console.log("Uint8Array.prototype.filter",typeof Uint8Array.prototype.filter);
   console.log("Uint8Array.prototype.find",typeof Uint8Array.prototype.find);
   console.log("Uint8Array.prototype.findIndex",typeof Uint8Array.prototype.findIndex);
   console.log("Uint8Array.prototype.forEach",typeof Uint8Array.prototype.forEach);
   console.log("Uint8Array.prototype.includes",typeof Uint8Array.prototype.includes);
   console.log("Uint8Array.prototype.indexOf",typeof Uint8Array.prototype.indexOf);
   console.log("Uint8Array.prototype.join",typeof Uint8Array.prototype.join);
   console.log("Uint8Array.prototype.keys",typeof Uint8Array.prototype.keys);
   console.log("Uint8Array.prototype.lastIndexOf",typeof Uint8Array.prototype.lastIndexOf);
   console.log("Uint8Array.prototype.map",typeof Uint8Array.prototype.map);
   console.log("Uint8Array.prototype.reduce",typeof Uint8Array.prototype.reduce);
   console.log("Uint8Array.prototype.reduceRight",typeof Uint8Array.prototype.reduceRight);
   console.log("Uint8Array.prototype.reverse",typeof Uint8Array.prototype.reverse);
   console.log("Uint8Array.prototype.set",typeof Uint8Array.prototype.set);
   console.log("Uint8Array.prototype.slice",typeof Uint8Array.prototype.slice);
   console.log("Uint8Array.prototype.some",typeof Uint8Array.prototype.some);
   console.log("Uint8Array.prototype.sort",typeof Uint8Array.prototype.sort);
   console.log("Uint8Array.prototype.subarray",typeof Uint8Array.prototype.subarray);
   console.log("Uint8Array.prototype.values",typeof Uint8Array.prototype.values);
   console.log("Uint8Array.prototype.toLocaleString",typeof Uint8Array.prototype.toLocaleString);
   console.log("Uint8Array.prototype.toString",typeof Uint8Array.prototype.toString);
   console.log("Uint8ClampedArray.BYTES_PER_ELEMENT",typeof Uint8ClampedArray.BYTES_PER_ELEMENT);
   console.log("Uint8ClampedArray.name",typeof Uint8ClampedArray.name);
   console.log("Uint8ClampedArray.from",typeof Uint8ClampedArray.from);
   console.log("Uint8ClampedArray.of",typeof Uint8ClampedArray.of);
   console.log("Uint8ClampedArray.prototype.copyWithin",typeof Uint8ClampedArray.prototype.copyWithin);
   console.log("Uint8ClampedArray.prototype.entries",typeof Uint8ClampedArray.prototype.entries);
   console.log("Uint8ClampedArray.prototype.every",typeof Uint8ClampedArray.prototype.every);
   console.log("Uint8ClampedArray.prototype.fill",typeof Uint8ClampedArray.prototype.fill);
   console.log("Uint8ClampedArray.prototype.filter",typeof Uint8ClampedArray.prototype.filter);
   console.log("Uint8ClampedArray.prototype.find",typeof Uint8ClampedArray.prototype.find);
   console.log("Uint8ClampedArray.prototype.findIndex",typeof Uint8ClampedArray.prototype.findIndex);
   console.log("Uint8ClampedArray.prototype.forEach",typeof Uint8ClampedArray.prototype.forEach);
   console.log("Uint8ClampedArray.prototype.includes",typeof Uint8ClampedArray.prototype.includes);
   console.log("Uint8ClampedArray.prototype.indexOf",typeof Uint8ClampedArray.prototype.indexOf);
   console.log("Uint8ClampedArray.prototype.join",typeof Uint8ClampedArray.prototype.join);
   console.log("Uint8ClampedArray.prototype.keys",typeof Uint8ClampedArray.prototype.keys);
   console.log("Uint8ClampedArray.prototype.lastIndexOf",typeof Uint8ClampedArray.prototype.lastIndexOf);
   console.log("Uint8ClampedArray.prototype.map",typeof Uint8ClampedArray.prototype.map);
   console.log("Uint8ClampedArray.prototype.reduce",typeof Uint8ClampedArray.prototype.reduce);
   console.log("Uint8ClampedArray.prototype.reduceRight",typeof Uint8ClampedArray.prototype.reduceRight);
   console.log("Uint8ClampedArray.prototype.reverse",typeof Uint8ClampedArray.prototype.reverse);
   console.log("Uint8ClampedArray.prototype.set",typeof Uint8ClampedArray.prototype.set);
   console.log("Uint8ClampedArray.prototype.slice",typeof Uint8ClampedArray.prototype.slice);
   console.log("Uint8ClampedArray.prototype.some",typeof Uint8ClampedArray.prototype.some);
   console.log("Uint8ClampedArray.prototype.sort",typeof Uint8ClampedArray.prototype.sort);
   console.log("Uint8ClampedArray.prototype.subarray",typeof Uint8ClampedArray.prototype.subarray);
   console.log("Uint8ClampedArray.prototype.values",typeof Uint8ClampedArray.prototype.values);
   console.log("Uint8ClampedArray.prototype.toLocaleString",typeof Uint8ClampedArray.prototype.toLocaleString);
   console.log("Uint8ClampedArray.prototype.toString",typeof Uint8ClampedArray.prototype.toString);
   console.log("Int16Array.BYTES_PER_ELEMENT",typeof Int16Array.BYTES_PER_ELEMENT);
   console.log("Int16Array.name",typeof Int16Array.name);
   console.log("Int16Array.from",typeof Int16Array.from);
   console.log("Int16Array.of",typeof Int16Array.of);
   console.log("Int16Array.prototype.copyWithin",typeof Int16Array.prototype.copyWithin);
   console.log("Int16Array.prototype.entries",typeof Int16Array.prototype.entries);
   console.log("Int16Array.prototype.every",typeof Int16Array.prototype.every);
   console.log("Int16Array.prototype.fill",typeof Int16Array.prototype.fill);
   console.log("Int16Array.prototype.filter",typeof Int16Array.prototype.filter);
   console.log("Int16Array.prototype.find",typeof Int16Array.prototype.find);
   console.log("Int16Array.prototype.findIndex",typeof Int16Array.prototype.findIndex);
   console.log("Int16Array.prototype.forEach",typeof Int16Array.prototype.forEach);
   console.log("Int16Array.prototype.includes",typeof Int16Array.prototype.includes);
   console.log("Int16Array.prototype.indexOf",typeof Int16Array.prototype.indexOf);
   console.log("Int16Array.prototype.join",typeof Int16Array.prototype.join);
   console.log("Int16Array.prototype.keys",typeof Int16Array.prototype.keys);
   console.log("Int16Array.prototype.lastIndexOf",typeof Int16Array.prototype.lastIndexOf);
   console.log("Int16Array.prototype.map",typeof Int16Array.prototype.map);
   console.log("Int16Array.prototype.reduce",typeof Int16Array.prototype.reduce);
   console.log("Int16Array.prototype.reduceRight",typeof Int16Array.prototype.reduceRight);
   console.log("Int16Array.prototype.reverse",typeof Int16Array.prototype.reverse);
   console.log("Int16Array.prototype.set",typeof Int16Array.prototype.set);
   console.log("Int16Array.prototype.slice",typeof Int16Array.prototype.slice);
   console.log("Int16Array.prototype.some",typeof Int16Array.prototype.some);
   console.log("Int16Array.prototype.sort",typeof Int16Array.prototype.sort);
   console.log("Int16Array.prototype.subarray",typeof Int16Array.prototype.subarray);
   console.log("Int16Array.prototype.values",typeof Int16Array.prototype.values);
   console.log("Int16Array.prototype.toLocaleString",typeof Int16Array.prototype.toLocaleString);
   console.log("Int16Array.prototype.toString",typeof Int16Array.prototype.toString);
   console.log("Uint16Array.BYTES_PER_ELEMENT",typeof Uint16Array.BYTES_PER_ELEMENT);
   console.log("Uint16Array.name",typeof Uint16Array.name);
   console.log("Uint16Array.from",typeof Uint16Array.from);
   console.log("Uint16Array.of",typeof Uint16Array.of);
   console.log("Uint16Array.prototype.copyWithin",typeof Uint16Array.prototype.copyWithin);
   console.log("Uint16Array.prototype.entries",typeof Uint16Array.prototype.entries);
   console.log("Uint16Array.prototype.every",typeof Uint16Array.prototype.every);
   console.log("Uint16Array.prototype.fill",typeof Uint16Array.prototype.fill);
   console.log("Uint16Array.prototype.filter",typeof Uint16Array.prototype.filter);
   console.log("Uint16Array.prototype.find",typeof Uint16Array.prototype.find);
   console.log("Uint16Array.prototype.findIndex",typeof Uint16Array.prototype.findIndex);
   console.log("Uint16Array.prototype.forEach",typeof Uint16Array.prototype.forEach);
   console.log("Uint16Array.prototype.includes",typeof Uint16Array.prototype.includes);
   console.log("Uint16Array.prototype.indexOf",typeof Uint16Array.prototype.indexOf);
   console.log("Uint16Array.prototype.join",typeof Uint16Array.prototype.join);
   console.log("Uint16Array.prototype.keys",typeof Uint16Array.prototype.keys);
   console.log("Uint16Array.prototype.lastIndexOf",typeof Uint16Array.prototype.lastIndexOf);
   console.log("Uint16Array.prototype.map",typeof Uint16Array.prototype.map);
   console.log("Uint16Array.prototype.reduce",typeof Uint16Array.prototype.reduce);
   console.log("Uint16Array.prototype.reduceRight",typeof Uint16Array.prototype.reduceRight);
   console.log("Uint16Array.prototype.reverse",typeof Uint16Array.prototype.reverse);
   console.log("Uint16Array.prototype.set",typeof Uint16Array.prototype.set);
   console.log("Uint16Array.prototype.slice",typeof Uint16Array.prototype.slice);
   console.log("Uint16Array.prototype.some",typeof Uint16Array.prototype.some);
   console.log("Uint16Array.prototype.sort",typeof Uint16Array.prototype.sort);
   console.log("Uint16Array.prototype.subarray",typeof Uint16Array.prototype.subarray);
   console.log("Uint16Array.prototype.values",typeof Uint16Array.prototype.values);
   console.log("Uint16Array.prototype.toLocaleString",typeof Uint16Array.prototype.toLocaleString);
   console.log("Uint16Array.prototype.toString",typeof Uint16Array.prototype.toString);
   console.log("Int32Array.BYTES_PER_ELEMENT",typeof Int32Array.BYTES_PER_ELEMENT);
   console.log("Int32Array.name",typeof Int32Array.name);
   console.log("Int32Array.from",typeof Int32Array.from);
   console.log("Int32Array.of",typeof Int32Array.of);
   console.log("Int32Array.prototype.copyWithin",typeof Int32Array.prototype.copyWithin);
   console.log("Int32Array.prototype.entries",typeof Int32Array.prototype.entries);
   console.log("Int32Array.prototype.every",typeof Int32Array.prototype.every);
   console.log("Int32Array.prototype.fill",typeof Int32Array.prototype.fill);
   console.log("Int32Array.prototype.filter",typeof Int32Array.prototype.filter);
   console.log("Int32Array.prototype.find",typeof Int32Array.prototype.find);
   console.log("Int32Array.prototype.findIndex",typeof Int32Array.prototype.findIndex);
   console.log("Int32Array.prototype.forEach",typeof Int32Array.prototype.forEach);
   console.log("Int32Array.prototype.includes",typeof Int32Array.prototype.includes);
   console.log("Int32Array.prototype.indexOf",typeof Int32Array.prototype.indexOf);
   console.log("Int32Array.prototype.join",typeof Int32Array.prototype.join);
   console.log("Int32Array.prototype.keys",typeof Int32Array.prototype.keys);
   console.log("Int32Array.prototype.lastIndexOf",typeof Int32Array.prototype.lastIndexOf);
   console.log("Int32Array.prototype.map",typeof Int32Array.prototype.map);
   console.log("Int32Array.prototype.reduce",typeof Int32Array.prototype.reduce);
   console.log("Int32Array.prototype.reduceRight",typeof Int32Array.prototype.reduceRight);
   console.log("Int32Array.prototype.reverse",typeof Int32Array.prototype.reverse);
   console.log("Int32Array.prototype.set",typeof Int32Array.prototype.set);
   console.log("Int32Array.prototype.slice",typeof Int32Array.prototype.slice);
   console.log("Int32Array.prototype.some",typeof Int32Array.prototype.some);
   console.log("Int32Array.prototype.sort",typeof Int32Array.prototype.sort);
   console.log("Int32Array.prototype.subarray",typeof Int32Array.prototype.subarray);
   console.log("Int32Array.prototype.values",typeof Int32Array.prototype.values);
   console.log("Int32Array.prototype.toLocaleString",typeof Int32Array.prototype.toLocaleString);
   console.log("Int32Array.prototype.toString",typeof Int32Array.prototype.toString);
   console.log("Uint32Array.BYTES_PER_ELEMENT",typeof Uint32Array.BYTES_PER_ELEMENT);
   console.log("Uint32Array.name",typeof Uint32Array.name);
   console.log("Uint32Array.from",typeof Uint32Array.from);
   console.log("Uint32Array.of",typeof Uint32Array.of);
   console.log("Uint32Array.prototype.copyWithin",typeof Uint32Array.prototype.copyWithin);
   console.log("Uint32Array.prototype.entries",typeof Uint32Array.prototype.entries);
   console.log("Uint32Array.prototype.every",typeof Uint32Array.prototype.every);
   console.log("Uint32Array.prototype.fill",typeof Uint32Array.prototype.fill);
   console.log("Uint32Array.prototype.filter",typeof Uint32Array.prototype.filter);
   console.log("Uint32Array.prototype.find",typeof Uint32Array.prototype.find);
   console.log("Uint32Array.prototype.findIndex",typeof Uint32Array.prototype.findIndex);
   console.log("Uint32Array.prototype.forEach",typeof Uint32Array.prototype.forEach);
   console.log("Uint32Array.prototype.includes",typeof Uint32Array.prototype.includes);
   console.log("Uint32Array.prototype.indexOf",typeof Uint32Array.prototype.indexOf);
   console.log("Uint32Array.prototype.join",typeof Uint32Array.prototype.join);
   console.log("Uint32Array.prototype.keys",typeof Uint32Array.prototype.keys);
   console.log("Uint32Array.prototype.lastIndexOf",typeof Uint32Array.prototype.lastIndexOf);
   console.log("Uint32Array.prototype.map",typeof Uint32Array.prototype.map);
   console.log("Uint32Array.prototype.reduce",typeof Uint32Array.prototype.reduce);
   console.log("Uint32Array.prototype.reduceRight",typeof Uint32Array.prototype.reduceRight);
   console.log("Uint32Array.prototype.reverse",typeof Uint32Array.prototype.reverse);
   console.log("Uint32Array.prototype.set",typeof Uint32Array.prototype.set);
   console.log("Uint32Array.prototype.slice",typeof Uint32Array.prototype.slice);
   console.log("Uint32Array.prototype.some",typeof Uint32Array.prototype.some);
   console.log("Uint32Array.prototype.sort",typeof Uint32Array.prototype.sort);
   console.log("Uint32Array.prototype.subarray",typeof Uint32Array.prototype.subarray);
   console.log("Uint32Array.prototype.values",typeof Uint32Array.prototype.values);
   console.log("Uint32Array.prototype.toLocaleString",typeof Uint32Array.prototype.toLocaleString);
   console.log("Uint32Array.prototype.toString",typeof Uint32Array.prototype.toString);
   console.log("Map.constructor",typeof Map.constructor);
   console.log("Map.prototype.clear",typeof Map.prototype.clear);
   console.log("Map.prototype.delete",typeof Map.prototype.delete);
   console.log("Map.prototype.get",typeof Map.prototype.get);
   console.log("Map.prototype.has",typeof Map.prototype.has);
   console.log("Map.prototype.set",typeof Map.prototype.set);
   console.log("Map.prototype.keys",typeof Map.prototype.keys);
   console.log("Map.prototype.values",typeof Map.prototype.values);
   console.log("Map.prototype.entries",typeof Map.prototype.entries);
   console.log("Map.prototype.forEach",typeof Map.prototype.forEach);
   console.log("Set.constructor",typeof Set.constructor);
   console.log("Set.prototype.add",typeof Set.prototype.add);
   console.log("Set.prototype.clear",typeof Set.prototype.clear);
   console.log("Set.prototype.delete",typeof Set.prototype.delete);
   console.log("Set.prototype.has",typeof Set.prototype.has);
   console.log("Set.prototype.keys",typeof Set.prototype.keys);
   console.log("Set.prototype.values",typeof Set.prototype.values);
   console.log("Set.prototype.entries",typeof Set.prototype.entries);
   console.log("Set.prototype.forEach",typeof Set.prototype.forEach);
   console.log("WeakMap.constructor",typeof WeakMap.constructor);
   console.log("WeakMap.prototype.delete",typeof WeakMap.prototype.delete);
   console.log("WeakMap.prototype.get",typeof WeakMap.prototype.get);
   console.log("WeakMap.prototype.has",typeof WeakMap.prototype.has);
   console.log("WeakMap.prototype.set",typeof WeakMap.prototype.set);
   console.log("WeakSet.constructor",typeof WeakSet.constructor);
   console.log("WeakSet.prototype.add",typeof WeakSet.prototype.add);
   console.log("WeakSet.prototype.delete",typeof WeakSet.prototype.delete);
   console.log("WeakSet.prototype.has",typeof WeakSet.prototype.has);

   console.log("exports",typeof exports);
   console.log("global",typeof global);
   console.log("process",typeof process);
   console.log("console",typeof console);
   console.log("require",typeof require);
}



dumpBuiltins();
nobaseDump(this,"");
nobaseDump(module,"module.");
nobaseDump(global,"");

nobaseDumpReq('assert');
nobaseDumpReq('buffer');
nobaseDumpReq('child_process');
nobaseDumpReq('cluster');
nobaseDumpReq('crypto');
nobaseDumpReq('dgram');
nobaseDumpReq('dns');
nobaseDumpReq('events');
nobaseDumpReq('fs');
nobaseDumpReq('http');
nobaseDumpReq('https');
nobaseDumpReq('net');
nobaseDumpReq('os');
nobaseDumpReq('path');
nobaseDumpReq('querystring');
nobaseDumpReq('readline');
nobaseDumpReq('stream');
nobaseDumpReq('string_decoder');
nobaseDumpReq('timers');
nobaseDumpReq('tls');
nobaseDumpReq('tty');
nobaseDumpReq('url');
nobaseDumpReq('util');
nobaseDumpReq('v8');
nobaseDumpReq('vm');
nobaseDumpReq('zlib');

