function nobaseTest() {
   for (var key in this) {
      if (key == 'nobaseTest') continue;
      console.log(key,typeof this[key]);
      if (typeof this[key] == 'object') {
	 for (var key1 in this[key]) {
	    console.log(key + "." + key1,typeof this[key][key1]);
	  }
       }
    }
}


var nobaseDone = { };

function nobaseDump(obj,pfx)
{
   for (var key in obj) {
      if (key == 'nobaseTest' || key == 'nobaseDump' || key == 'nobaseDone') continue;
      if (nobaseDone[obj[key]] == true) continue;
      if (typeof obj[key] == 'string' && key != '_eval') {
	 console.log(pfx + key,typeof obj[key],obj[key]);
       }
      else if (Array.isArray(obj[key])) {
	 console.log(pfx + key,"Array",obj[key].length);
       }
      else {
	 console.log(pfx + key,typeof obj[key]);
       }
    }

   for (var key in obj) {
      if (key == 'nobaseTest' || key == 'nobaseDump') continue;
      if (nobaseDone[obj[key]] == true) continue;
      nobaseDone[obj[key]] = true;
      if (obj[key] === obj) continue;
      if (typeof obj[key] == 'object') {
	 if (!Array.isArray(obj[key])) {
	    nobaseDump(obj[key],pfx + key + ".");
	  }
       }
    }

}




console.log("MODULE",module,typeof module);
console.log("MODULE.exports",module.exports,typeof module.exports);
console.log("EXPORTS",exports,typeof exports);
console.log("REQUIRE",require,typeof require);
nobaseDump(module,"Module.");

