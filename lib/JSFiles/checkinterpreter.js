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
   console.log("parseint",typeof parseint);
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
   console.log("Error",typeof Error);
   console.log("EvalError",typeof EvalError);
   console.log("RangeError",typeof RangeError);
   console.log("ReferenceError",typeof ReferenceError);
   console.log("SyntaxError",typeof SyntaxError);
   console.log("TypeError",typeof TypeError);
   console.log("URIError",typeof URIError);
   console.log("Math",typeof Math);
   console.log("JSON",typeof JSON);
   console.log("Object.constructor",typeof Object.constructor);
   console.log("Object.prototype",typeof Object.prototype);
   console.log("Object.getPrototypeOf",typeof Object.getPrototypeOf);
   console.log("Object.getOwnPropertyDescriptor",typeof Object.getOwnPropertyDescriptor);
   console.log("Object.getOwnPropertyNames",typeof Object.getOwnPropertyNames);
   console.log("Object.create",typeof Object.create);
   console.log("Object.defineProperty",typeof Object.defineProperty);
   console.log("Object.defineProperties",typeof Object.defineProperties);
   console.log("Object.seal",typeof Object.seal);
   console.log("Object.freeze",typeof Object.freeze);
   console.log("Object.preventExtensions",typeof Object.preventExtensions);
   console.log("Object.isSealed",typeof Object.isSealed);
   console.log("Object.isFrozen",typeof Object.isFrozen);
   console.log("Object.isExtensible",typeof Object.isExtensible);
   console.log("Object.keys",typeof Object.keys);
   console.log("Object.prototype.constructor",typeof Object.prototype.constructor);
   console.log("Object.prototype.toString",typeof Object.prototype.toString);
   console.log("Object.prototype.toLocaleString",typeof Object.prototype.toLocaleString);
   console.log("Object.prototype.valueOf",typeof Object.prototype.valueOf);
   console.log("Object.prototype.hasOwnProperty",typeof Object.prototype.hasOwnProperty);
   console.log("Object.prototype.isPrototypeOf",typeof Object.prototype.isPrototypeOf);
   console.log("Object.prototype.PropertyIsEnumerable",typeof Object.prototype.PropertyIsEnumerable);
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
   console.log("Array.prototype.constructor",typeof Array.prototype.constructor);
   console.log("Array.prototype.toString",typeof Array.prototype.toString);
   console.log("Array.prototype.toLocaleString",typeof Array.prototype.toLocaleString);
   console.log("Array.prototype.concat",typeof Array.prototype.concat);
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
   console.log("Array.prototype.every",typeof Array.prototype.every);
   console.log("Array.prototype.some",typeof Array.prototype.some);
   console.log("Array.prototype.map",typeof Array.prototype.map);
   console.log("Array.prototype.filter",typeof Array.prototype.filter);
   console.log("Array.prototype.reduce",typeof Array.prototype.reduce);
   console.log("Array.prototype.reduceRight",typeof Array.prototype.reduceRight);
   console.log("Array.length",typeof Array.length);
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
   console.log("String.prototype.search",typeof String.prototype.search);
   console.log("String.prototype.slice",typeof String.prototype.slice);
   console.log("String.prototype.split",typeof String.prototype.split);
   console.log("String.prototype.substring",typeof String.prototype.substring);
   console.log("String.prototype.toLowerCase",typeof String.prototype.toLowerCase);
   console.log("String.prototype.toLocaleLowerCase",typeof String.prototype.toLocaleLowerCase);
   console.log("String.prototype.toUpperCase",typeof String.prototype.toUpperCase);
   console.log("String.prototype.toLocaleUpperCase",typeof String.prototype.toLocaleUpperCase);
   console.log("String.prototype.trim",typeof String.prototype.trim);
   console.log("String.length",typeof String.length);
   console.log("Boolean.constructor",typeof Boolean.constructor);
   console.log("Boolean.prototype",typeof Boolean.prototype);
   console.log("Boolean.prototype.constructor",typeof Boolean.prototype.constructor);
   console.log("Boolean.prototype.toString",typeof Boolean.prototype.toString);
   console.log("Boolean.prototype.valueOf",typeof Boolean.prototype.valueOf);
   console.log("Number.constructor",typeof Number.constructor);
   console.log("Number.prototype",typeof Number.prototype);
   console.log("Number.MAX_VALUE",typeof Number.MAX_VALUE);
   console.log("Number.MIN_VALUE",typeof Number.MIN_VALUE);
   console.log("Number.NaN",typeof Number.NaN);
   console.log("Number.NEGATIVE_INFINITY",typeof Number.NEGATIVE_INFINITY);
   console.log("Number.POSITIVE_INFINITY",typeof Number.POSITIVE_INFINITY);
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

