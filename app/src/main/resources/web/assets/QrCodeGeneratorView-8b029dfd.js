import{_ as J}from"./MonacoEditor.vuevuetypescriptsetuptruelang-232f22d7.js";import{_ as Y}from"./Breadcrumb-754c867d.js";import{g as z,M as j}from"./splitpanes.es-c29164c5.js";import{b7 as d,d as q,r as M,G as tt,o as P,c as H,b as O,e as x,v as C,g as S,k as rt,_ as et}from"./index-79f9263f.js";/**
* @module QRCode
* @package @nuintun/qrcode
* @license MIT
* @version 3.3.0
* @author nuintun <nuintun@qq.com>
* @description A pure JavaScript QRCode encode and decode library.
* @see https://github.com/nuintun/qrcode#readme
*/var p;(function(r){r[r.Terminator=0]="Terminator",r[r.Numeric=1]="Numeric",r[r.Alphanumeric=2]="Alphanumeric",r[r.StructuredAppend=3]="StructuredAppend",r[r.Byte=4]="Byte",r[r.Kanji=8]="Kanji",r[r.ECI=7]="ECI"})(p||(p={}));/**
* @module QRCode
* @package @nuintun/qrcode
* @license MIT
* @version 3.3.0
* @author nuintun <nuintun@qq.com>
* @description A pure JavaScript QRCode encode and decode library.
* @see https://github.com/nuintun/qrcode#readme
*/var Q=function(){function r(t,e){this.bytes=[],this.mode=t,this.data=e}return r.prototype.getLength=function(){return this.bytes.length},r.prototype.getLengthInBits=function(t){var e=this.mode,i=new Error("illegal mode: ".concat(e));if(1<=t&&t<10)switch(e){case p.Numeric:return 10;case p.Alphanumeric:return 9;case p.Byte:return 8;case p.Kanji:return 8;default:throw i}else if(t<27)switch(e){case p.Numeric:return 12;case p.Alphanumeric:return 11;case p.Byte:return 16;case p.Kanji:return 10;default:throw i}else if(t<41)switch(e){case p.Numeric:return 14;case p.Alphanumeric:return 13;case p.Byte:return 16;case p.Kanji:return 12;default:throw i}else throw new Error("illegal version: ".concat(t))},r}();/**
* @module QRCode
* @package @nuintun/qrcode
* @license MIT
* @version 3.3.0
* @author nuintun <nuintun@qq.com>
* @description A pure JavaScript QRCode encode and decode library.
* @see https://github.com/nuintun/qrcode#readme
*/function nt(r){for(var t=0,e=r.length,i=[],n=0;n<e;n++){var a=r.charCodeAt(n);a<128?i[t++]=a:a<2048?(i[t++]=a>>6|192,i[t++]=a&63|128):(a&64512)===55296&&n+1<e&&(r.charCodeAt(n+1)&64512)===56320?(a=65536+((a&1023)<<10)+(r.charCodeAt(++n)&1023),i[t++]=a>>18|240,i[t++]=a>>12&63|128,i[t++]=a>>6&63|128,i[t++]=a&63|128):(i[t++]=a>>12|224,i[t++]=a>>6&63|128,i[t++]=a&63|128)}return i}/**
* @module QRCode
* @package @nuintun/qrcode
* @license MIT
* @version 3.3.0
* @author nuintun <nuintun@qq.com>
* @description A pure JavaScript QRCode encode and decode library.
* @see https://github.com/nuintun/qrcode#readme
*/var it=function(r){d(t,r);function t(e,i){var n=r.call(this,p.Byte,e)||this;if(n.encoding=-1,typeof i=="function"){var a=i(e),s=a.encoding,f=a.bytes;n.bytes=f,n.encoding=s}else n.bytes=nt(e),n.encoding=26;return n}return t.prototype.writeTo=function(e){for(var i=this.bytes,n=0,a=i;n<a.length;n++){var s=a[n];e.put(s,8)}},t}(Q);/**
* @module QRCode
* @package @nuintun/qrcode
* @license MIT
* @version 3.3.0
* @author nuintun <nuintun@qq.com>
* @description A pure JavaScript QRCode encode and decode library.
* @see https://github.com/nuintun/qrcode#readme
*/var B=[],I=[];for(var w=0;w<256;w++)I[w]=0,B[w]=w<8?1<<w:B[w-4]^B[w-5]^B[w-6]^B[w-8];for(var w=0;w<255;w++)I[B[w]]=w;function _(r){if(r<1)throw new Error("illegal log: ".concat(r));return I[r]}function D(r){for(;r<0;)r+=255;for(;r>=256;)r-=255;return B[r]}/**
* @module QRCode
* @package @nuintun/qrcode
* @license MIT
* @version 3.3.0
* @author nuintun <nuintun@qq.com>
* @description A pure JavaScript QRCode encode and decode library.
* @see https://github.com/nuintun/qrcode#readme
*/var A=function(){function r(t,e){e===void 0&&(e=0);for(var i=0,n=t.length;i<n&&t[i]===0;)i++;n-=i;for(var a=[],s=0;s<n;s++)a.push(t[i+s]);for(var s=0;s<e;s++)a.push(0);this.num=a}return r.prototype.getAt=function(t){return this.num[t]},r.prototype.getLength=function(){return this.num.length},r.prototype.multiply=function(t){for(var e=[],i=t.getLength(),n=this.getLength(),a=n+i-1,s=0;s<a;s++)e.push(0);for(var s=0;s<n;s++)for(var f=0;f<i;f++)e[s+f]^=D(_(this.getAt(s))+_(t.getAt(f)));return new r(e)},r.prototype.mod=function(t){var e=t.getLength(),i=this.getLength();if(i-e<0)return this;for(var n=_(this.getAt(0))-_(t.getAt(0)),a=[],s=0;s<i;s++)a.push(this.getAt(s));for(var s=0;s<e;s++)a[s]^=D(_(t.getAt(s))+n);return new r(a).mod(t)},r}();/**
* @module QRCode
* @package @nuintun/qrcode
* @license MIT
* @version 3.3.0
* @author nuintun <nuintun@qq.com>
* @description A pure JavaScript QRCode encode and decode library.
* @see https://github.com/nuintun/qrcode#readme
*/var at=3,st=3,V=40,ot=10,ft=[[],[6,18],[6,22],[6,26],[6,30],[6,34],[6,22,38],[6,24,42],[6,26,46],[6,28,50],[6,30,54],[6,32,58],[6,34,62],[6,26,46,66],[6,26,48,70],[6,26,50,74],[6,30,54,78],[6,30,56,82],[6,30,58,86],[6,34,62,90],[6,28,50,72,94],[6,26,50,74,98],[6,30,54,78,102],[6,28,54,80,106],[6,32,58,84,110],[6,30,58,86,114],[6,34,62,90,118],[6,26,50,74,98,122],[6,30,54,78,102,126],[6,26,52,78,104,130],[6,30,56,82,108,134],[6,34,60,86,112,138],[6,30,58,86,114,142],[6,34,62,90,118,146],[6,30,54,78,102,126,150],[6,24,50,76,102,128,154],[6,28,54,80,106,132,158],[6,32,58,84,110,136,162],[6,26,54,82,110,138,166],[6,30,58,86,114,142,170]],ut=1<<14|1<<12|1<<10|1<<4|1<<1,U=1<<10|1<<8|1<<5|1<<4|1<<2|1<<1|1<<0,W=1<<12|1<<11|1<<10|1<<9|1<<8|1<<5|1<<2|1<<0;function ht(r){return ft[r-1]}function vt(r){for(var t=new A([1]),e=0;e<r;e++)t=t.multiply(new A([1,D(e)]));return t}function L(r){for(var t=0;r!==0;)t++,r>>>=1;return t}var N=L(W);function lt(r){for(var t=r<<12;L(t)-N>=0;)t^=W<<L(t)-N;return r<<12|t}var G=L(U);function ct(r){for(var t=r<<10;L(t)-G>=0;)t^=U<<L(t)-G;return(r<<10|t)^ut}function F(r,t){for(var e=r.getMatrixSize(),i=0,n=0;n<e;n++)for(var a=!1,s=0,f=0;f<e;f++){var u=t?r.isDark(n,f):r.isDark(f,n);u===a?(s++,s===5?i+=at:s>5&&i++):(a=u,s=1)}return i}function pt(r){return F(r,!0)+F(r,!1)}function gt(r){for(var t=r.getMatrixSize(),e=0,i=0;i<t-1;i++)for(var n=0;n<t-1;n++){var a=r.isDark(i,n);a===r.isDark(i,n+1)&&a===r.isDark(i+1,n)&&a===r.isDark(i+1,n+1)&&(e+=st)}return e}function k(r,t,e,i,n){e=Math.max(e,0),i=Math.min(i,r.getMatrixSize());for(var a=e;a<i;a++){var s=n?r.isDark(t,a):r.isDark(a,t);if(s)return!1}return!0}function wt(r){for(var t=r.getMatrixSize(),e=0,i=0;i<t;i++)for(var n=0;n<t;n++)n+6<t&&r.isDark(i,n)&&!r.isDark(i,n+1)&&r.isDark(i,n+2)&&r.isDark(i,n+3)&&r.isDark(i,n+4)&&!r.isDark(i,n+5)&&r.isDark(i,n+6)&&(k(r,i,n-4,n,!0)||k(r,i,n+7,n+11,!0))&&(e+=V),i+6<t&&r.isDark(i,n)&&!r.isDark(i+1,n)&&r.isDark(i+2,n)&&r.isDark(i+3,n)&&r.isDark(i+4,n)&&!r.isDark(i+5,n)&&r.isDark(i+6,n)&&(k(r,n,i-4,i,!1)||k(r,n,i+7,i+11,!1))&&(e+=V);return e}function mt(r){for(var t=r.getMatrixSize(),e=0,i=0;i<t;i++)for(var n=0;n<t;n++)r.isDark(i,n)&&e++;var a=t*t,s=Math.floor(Math.abs(e*20-a*10)/a);return s*ot}function yt(r){return pt(r)+gt(r)+wt(r)+mt(r)}/**
* @module QRCode
* @package @nuintun/qrcode
* @license MIT
* @version 3.3.0
* @author nuintun <nuintun@qq.com>
* @description A pure JavaScript QRCode encode and decode library.
* @see https://github.com/nuintun/qrcode#readme
*/var y;(function(r){r[r.L=1]="L",r[r.M=0]="M",r[r.Q=3]="Q",r[r.H=2]="H"})(y||(y={}));/**
* @module QRCode
* @package @nuintun/qrcode
* @license MIT
* @version 3.3.0
* @author nuintun <nuintun@qq.com>
* @description A pure JavaScript QRCode encode and decode library.
* @see https://github.com/nuintun/qrcode#readme
*/var Bt=function(){function r(t,e){this.dataCount=e,this.totalCount=t}return r.prototype.getDataCount=function(){return this.dataCount},r.prototype.getTotalCount=function(){return this.totalCount},r.getRSBlocks=function(t,e){for(var i=[],n=r.getRSBlockTable(t,e),a=n.length/3,s=0;s<a;s++)for(var f=n[s*3+0],u=n[s*3+1],o=n[s*3+2],v=0;v<f;v++)i.push(new r(u,o));return i},r.getRSBlockTable=function(t,e){switch(e){case y.L:return r.RS_BLOCK_TABLE[(t-1)*4+0];case y.M:return r.RS_BLOCK_TABLE[(t-1)*4+1];case y.Q:return r.RS_BLOCK_TABLE[(t-1)*4+2];case y.H:return r.RS_BLOCK_TABLE[(t-1)*4+3];default:throw new Error("illegal error correction level: ".concat(e))}},r.RS_BLOCK_TABLE=[[1,26,19],[1,26,16],[1,26,13],[1,26,9],[1,44,34],[1,44,28],[1,44,22],[1,44,16],[1,70,55],[1,70,44],[2,35,17],[2,35,13],[1,100,80],[2,50,32],[2,50,24],[4,25,9],[1,134,108],[2,67,43],[2,33,15,2,34,16],[2,33,11,2,34,12],[2,86,68],[4,43,27],[4,43,19],[4,43,15],[2,98,78],[4,49,31],[2,32,14,4,33,15],[4,39,13,1,40,14],[2,121,97],[2,60,38,2,61,39],[4,40,18,2,41,19],[4,40,14,2,41,15],[2,146,116],[3,58,36,2,59,37],[4,36,16,4,37,17],[4,36,12,4,37,13],[2,86,68,2,87,69],[4,69,43,1,70,44],[6,43,19,2,44,20],[6,43,15,2,44,16],[4,101,81],[1,80,50,4,81,51],[4,50,22,4,51,23],[3,36,12,8,37,13],[2,116,92,2,117,93],[6,58,36,2,59,37],[4,46,20,6,47,21],[7,42,14,4,43,15],[4,133,107],[8,59,37,1,60,38],[8,44,20,4,45,21],[12,33,11,4,34,12],[3,145,115,1,146,116],[4,64,40,5,65,41],[11,36,16,5,37,17],[11,36,12,5,37,13],[5,109,87,1,110,88],[5,65,41,5,66,42],[5,54,24,7,55,25],[11,36,12,7,37,13],[5,122,98,1,123,99],[7,73,45,3,74,46],[15,43,19,2,44,20],[3,45,15,13,46,16],[1,135,107,5,136,108],[10,74,46,1,75,47],[1,50,22,15,51,23],[2,42,14,17,43,15],[5,150,120,1,151,121],[9,69,43,4,70,44],[17,50,22,1,51,23],[2,42,14,19,43,15],[3,141,113,4,142,114],[3,70,44,11,71,45],[17,47,21,4,48,22],[9,39,13,16,40,14],[3,135,107,5,136,108],[3,67,41,13,68,42],[15,54,24,5,55,25],[15,43,15,10,44,16],[4,144,116,4,145,117],[17,68,42],[17,50,22,6,51,23],[19,46,16,6,47,17],[2,139,111,7,140,112],[17,74,46],[7,54,24,16,55,25],[34,37,13],[4,151,121,5,152,122],[4,75,47,14,76,48],[11,54,24,14,55,25],[16,45,15,14,46,16],[6,147,117,4,148,118],[6,73,45,14,74,46],[11,54,24,16,55,25],[30,46,16,2,47,17],[8,132,106,4,133,107],[8,75,47,13,76,48],[7,54,24,22,55,25],[22,45,15,13,46,16],[10,142,114,2,143,115],[19,74,46,4,75,47],[28,50,22,6,51,23],[33,46,16,4,47,17],[8,152,122,4,153,123],[22,73,45,3,74,46],[8,53,23,26,54,24],[12,45,15,28,46,16],[3,147,117,10,148,118],[3,73,45,23,74,46],[4,54,24,31,55,25],[11,45,15,31,46,16],[7,146,116,7,147,117],[21,73,45,7,74,46],[1,53,23,37,54,24],[19,45,15,26,46,16],[5,145,115,10,146,116],[19,75,47,10,76,48],[15,54,24,25,55,25],[23,45,15,25,46,16],[13,145,115,3,146,116],[2,74,46,29,75,47],[42,54,24,1,55,25],[23,45,15,28,46,16],[17,145,115],[10,74,46,23,75,47],[10,54,24,35,55,25],[19,45,15,35,46,16],[17,145,115,1,146,116],[14,74,46,21,75,47],[29,54,24,19,55,25],[11,45,15,46,46,16],[13,145,115,6,146,116],[14,74,46,23,75,47],[44,54,24,7,55,25],[59,46,16,1,47,17],[12,151,121,7,152,122],[12,75,47,26,76,48],[39,54,24,14,55,25],[22,45,15,41,46,16],[6,151,121,14,152,122],[6,75,47,34,76,48],[46,54,24,10,55,25],[2,45,15,64,46,16],[17,152,122,4,153,123],[29,74,46,14,75,47],[49,54,24,10,55,25],[24,45,15,46,46,16],[4,152,122,18,153,123],[13,74,46,32,75,47],[48,54,24,14,55,25],[42,45,15,32,46,16],[20,147,117,4,148,118],[40,75,47,7,76,48],[43,54,24,22,55,25],[10,45,15,67,46,16],[19,148,118,6,149,119],[18,75,47,31,76,48],[34,54,24,34,55,25],[20,45,15,61,46,16]],r}();/**
* @module QRCode
* @package @nuintun/qrcode
* @license MIT
* @version 3.3.0
* @author nuintun <nuintun@qq.com>
* @description A pure JavaScript QRCode encode and decode library.
* @see https://github.com/nuintun/qrcode#readme
*/var Z=function(){function r(){this.length=0,this.buffer=[]}return r.prototype.getBuffer=function(){return this.buffer},r.prototype.getLengthInBits=function(){return this.length},r.prototype.getBit=function(t){return(this.buffer[t/8>>0]>>>7-t%8&1)===1},r.prototype.put=function(t,e){for(var i=0;i<e;i++)this.putBit((t>>>e-i-1&1)===1)},r.prototype.putBit=function(t){var e=this.buffer;this.length===e.length*8&&e.push(0),t&&(e[this.length/8>>0]|=128>>>this.length%8),this.length++},r}();/**
* @module QRCode
* @package @nuintun/qrcode
* @license MIT
* @version 3.3.0
* @author nuintun <nuintun@qq.com>
* @description A pure JavaScript QRCode encode and decode library.
* @see https://github.com/nuintun/qrcode#readme
*/var X=function(){function r(){}return r.prototype.writeBytes=function(t,e,i){e===void 0&&(e=0),i===void 0&&(i=t.length);for(var n=0;n<i;n++)this.writeByte(t[n+e])},r.prototype.flush=function(){},r.prototype.close=function(){this.flush()},r}();/**
* @module QRCode
* @package @nuintun/qrcode
* @license MIT
* @version 3.3.0
* @author nuintun <nuintun@qq.com>
* @description A pure JavaScript QRCode encode and decode library.
* @see https://github.com/nuintun/qrcode#readme
*/var E=function(r){d(t,r);function t(){var e=r!==null&&r.apply(this,arguments)||this;return e.bytes=[],e}return t.prototype.writeByte=function(e){this.bytes.push(e)},t.prototype.writeInt16=function(e){this.bytes.push(e,e>>>8)},t.prototype.toByteArray=function(){return this.bytes},t}(X);/**
* @module QRCode
* @package @nuintun/qrcode
* @license MIT
* @version 3.3.0
* @author nuintun <nuintun@qq.com>
* @description A pure JavaScript QRCode encode and decode library.
* @see https://github.com/nuintun/qrcode#readme
*/function Lt(r){if(r>=0){if(r<26)return 65+r;if(r<52)return 97+(r-26);if(r<62)return 48+(r-52);if(r===62)return 43;if(r===63)return 47}throw new Error("illegal char: ".concat(String.fromCharCode(r)))}var xt=function(r){d(t,r);function t(e){var i=r.call(this)||this;return i.buffer=0,i.length=0,i.bufLength=0,i.stream=e,i}return t.prototype.writeByte=function(e){for(this.buffer=this.buffer<<8|e&255,this.bufLength+=8,this.length++;this.bufLength>=6;)this.writeEncoded(this.buffer>>>this.bufLength-6),this.bufLength-=6},t.prototype.flush=function(){this.bufLength>0&&(this.writeEncoded(this.buffer<<6-this.bufLength),this.buffer=0,this.bufLength=0);var e=this.stream;if(this.length%3!=0)for(var i=3-this.length%3,n=0;n<i;n++)e.writeByte(61)},t.prototype.writeEncoded=function(e){this.stream.writeByte(Lt(e&63))},t}(X);/**
* @module QRCode
* @package @nuintun/qrcode
* @license MIT
* @version 3.3.0
* @author nuintun <nuintun@qq.com>
* @description A pure JavaScript QRCode encode and decode library.
* @see https://github.com/nuintun/qrcode#readme
*/function _t(r){var t=new E,e=new xt(t);return e.writeBytes(r),e.close(),t.close(),t.toByteArray()}var bt=function(){function r(){this.size=0,this.map={}}return r.prototype.add=function(t){this.contains(t)||(this.map[t]=this.size++)},r.prototype.getSize=function(){return this.size},r.prototype.indexOf=function(t){return this.map[t]},r.prototype.contains=function(t){return this.map[t]>=0},r}(),kt=function(){function r(t){this.output=t,this.bitLength=0,this.bitBuffer=0}return r.prototype.write=function(t,e){if(t>>>e)throw new Error("length overflow");for(var i=this.output;this.bitLength+e>=8;)i.writeByte(255&(t<<this.bitLength|this.bitBuffer)),e-=8-this.bitLength,t>>>=8-this.bitLength,this.bitBuffer=0,this.bitLength=0;this.bitBuffer=t<<this.bitLength|this.bitBuffer,this.bitLength=this.bitLength+e},r.prototype.flush=function(){var t=this.output;this.bitLength>0&&t.writeByte(this.bitBuffer),t.flush()},r.prototype.close=function(){this.flush(),this.output.close()},r}(),Ct=function(){function r(t,e){this.data=[],this.width=t,this.height=e;for(var i=t*e,n=0;n<i;n++)this.data[n]=0}return r.prototype.getLZWRaster=function(t){for(var e=new bt,i=String.fromCharCode,n=1<<t,a=(1<<t)+1,s=0;s<n;s++)e.add(i(s));e.add(i(n)),e.add(i(a));var f=t+1,u=new E,o=new kt(u);try{var v=this.data,l=v.length,m=String.fromCharCode;o.write(n,f);for(var h=0,c=m(v[h++]);h<l;){var g=m(v[h++]);e.contains(c+g)?c+=g:(o.write(e.indexOf(c),f),e.getSize()<4095&&(e.getSize()===1<<f&&f++,e.add(c+g)),c=g)}o.write(e.indexOf(c),f),o.write(a,f)}finally{o.close()}return u.toByteArray()},r.prototype.setPixel=function(t,e,i){var n=this,a=n.width,s=n.height;if(t<0||a<=t)throw new Error("illegal x axis: ".concat(t));if(e<0||s<=e)throw new Error("illegal y axis: ".concat(e));this.data[e*a+t]=i},r.prototype.getPixel=function(t,e){var i=this,n=i.width,a=i.height;if(t<0||n<=t)throw new Error("illegal x axis: ".concat(t));if(e<0||a<=e)throw new Error("illegal y axis: ".concat(e));return this.data[e*n+t]},r.prototype.write=function(t){var e=this,i=e.width,n=e.height;t.writeByte(71),t.writeByte(73),t.writeByte(70),t.writeByte(56),t.writeByte(55),t.writeByte(97),t.writeInt16(i),t.writeInt16(n),t.writeByte(128),t.writeByte(0),t.writeByte(0),t.writeByte(0),t.writeByte(0),t.writeByte(0),t.writeByte(255),t.writeByte(255),t.writeByte(255),t.writeByte(44),t.writeInt16(0),t.writeInt16(0),t.writeInt16(i),t.writeInt16(n),t.writeByte(0);var a=2,s=this.getLZWRaster(a),f=s.length;t.writeByte(a);for(var u=0;f-u>255;)t.writeByte(255),t.writeBytes(s,u,255),u+=255;var o=f-u;t.writeByte(o),t.writeBytes(s,u,o),t.writeByte(0),t.writeByte(59)},r.prototype.toDataURL=function(){var t=new E;this.write(t);var e=_t(t.toByteArray());t.close();for(var i=e.length,n=String.fromCharCode,a="data:image/gif;base64,",s=0;s<i;s++)a+=n(e[s]);return a},r}();/**
* @module QRCode
* @package @nuintun/qrcode
* @license MIT
* @version 3.3.0
* @author nuintun <nuintun@qq.com>
* @description A pure JavaScript QRCode encode and decode library.
* @see https://github.com/nuintun/qrcode#readme
*/function St(r){switch(r){case 0:return function(t,e){return(t+e&1)===0};case 1:return function(t,e){return(e&1)===0};case 2:return function(t,e){return t%3===0};case 3:return function(t,e){return(t+e)%3===0};case 4:return function(t,e){return((t/3>>0)+(e/2>>0)&1)===0};case 5:return function(t,e){return(t*e&1)+t*e%3===0};case 6:return function(t,e){return((t*e&1)+t*e%3&1)===0};case 7:return function(t,e){return(t*e%3+(t+e&1)&1)===0};default:throw new Error("illegal mask: ".concat(r))}}/**
* @module QRCode
* @package @nuintun/qrcode
* @license MIT
* @version 3.3.0
* @author nuintun <nuintun@qq.com>
* @description A pure JavaScript QRCode encode and decode library.
* @see https://github.com/nuintun/qrcode#readme
*/var Dt=236,At=17,Et=Object.prototype.toString;function dt(r,t){if(r<0||r>=1e6)throw new Error("byte mode encoding hint out of range");t.put(p.ECI,4),r<1<<7?t.put(r,8):r<1<<14?(t.put(2,2),t.put(r,14)):(t.put(6,3),t.put(r,21))}function K(r,t,e,i){for(var n=new Z,a=Bt.getRSBlocks(r,t),s=0,f=i;s<f.length;s++){var u=f[s],o=u.mode;e&&o===p.Byte&&dt(u.encoding,n),n.put(o,4),n.put(u.getLength(),u.getLengthInBits(r)),u.writeTo(n)}for(var v=0,l=0,m=a;l<m.length;l++){var h=m[l];v+=h.getDataCount()}return v*=8,[n,a,v]}function It(r,t){for(var e=0,i=0,n=0,a=[],s=[],f=t.length,u=r.getBuffer(),o=0;o<f;o++){var v=t[o],l=v.getDataCount(),m=v.getTotalCount()-l;i=Math.max(i,l),n=Math.max(n,m),a[o]=[];for(var h=0;h<l;h++)a[o][h]=255&u[h+e];e+=l;var c=vt(m),g=c.getLength()-1,b=new A(a[o],g),R=b.mod(c),$=R.getLength();s[o]=[];for(var h=0;h<g;h++){var T=h+$-g;s[o][h]=T>=0?R.getAt(T):0}}r=new Z;for(var h=0;h<i;h++)for(var o=0;o<f;o++)h<a[o].length&&r.put(a[o][h],8);for(var h=0;h<n;h++)for(var o=0;o<f;o++)h<s[o].length&&r.put(s[o][h],8);return r}function Rt(r,t,e){for(r.getLengthInBits()+4<=e&&r.put(0,4);r.getLengthInBits()%8!==0;)r.putBit(!1);for(;!(r.getLengthInBits()>=e||(r.put(Dt,8),r.getLengthInBits()>=e));)r.put(At,8);return It(r,t)}var Tt=function(){function r(t){t===void 0&&(t={}),this.matrixSize=0,this.chunks=[],this.matrix=[];var e=t.version,i=e===void 0?0:e,n=t.encodingHint,a=n===void 0?!1:n,s=t.errorCorrectionLevel,f=s===void 0?y.L:s;this.setVersion(i),this.setEncodingHint(a),this.setErrorCorrectionLevel(f)}return r.prototype.getMatrix=function(){return this.matrix},r.prototype.getMatrixSize=function(){return this.matrixSize},r.prototype.getVersion=function(){return this.version},r.prototype.setVersion=function(t){return this.version=Math.min(40,Math.max(0,t>>0)),this.auto=this.version===0,this},r.prototype.getErrorCorrectionLevel=function(){return this.errorCorrectionLevel},r.prototype.setErrorCorrectionLevel=function(t){switch(t){case y.L:case y.M:case y.Q:case y.H:this.errorCorrectionLevel=t}return this},r.prototype.getEncodingHint=function(){return this.encodingHint},r.prototype.setEncodingHint=function(t){return this.encodingHint=t,this},r.prototype.write=function(t){var e=this.chunks;if(t instanceof Q)e.push(t);else{var i=Et.call(t);if(i==="[object String]")e.push(new it(t));else throw new Error("illegal data: ".concat(t))}return this},r.prototype.isDark=function(t,e){return this.matrix[t][e]===!0},r.prototype.setupFinderPattern=function(t,e){for(var i=this.matrix,n=this.matrixSize,a=-1;a<=7;a++)for(var s=-1;s<=7;s++)t+a<=-1||n<=t+a||e+s<=-1||n<=e+s||(0<=a&&a<=6&&(s===0||s===6)||0<=s&&s<=6&&(a===0||a===6)||2<=a&&a<=4&&2<=s&&s<=4?i[t+a][e+s]=!0:i[t+a][e+s]=!1)},r.prototype.setupAlignmentPattern=function(){for(var t=this.matrix,e=ht(this.version),i=e.length,n=0;n<i;n++)for(var a=0;a<i;a++){var s=e[n],f=e[a];if(t[s][f]===null)for(var u=-2;u<=2;u++)for(var o=-2;o<=2;o++)u===-2||u===2||o===-2||o===2||u===0&&o===0?t[s+u][f+o]=!0:t[s+u][f+o]=!1}},r.prototype.setupTimingPattern=function(){for(var t=this.matrix,e=this.matrixSize-8,i=8;i<e;i++){var n=i%2===0;t[i][6]===null&&(t[i][6]=n),t[6][i]===null&&(t[6][i]=n)}},r.prototype.setupFormatInfo=function(t){for(var e=this.matrix,i=this.errorCorrectionLevel<<3|t,n=ct(i),a=this.matrixSize,s=0;s<15;s++){var f=(n>>s&1)===1;s<6?e[s][8]=f:s<8?e[s+1][8]=f:e[a-15+s][8]=f,s<8?e[8][a-s-1]=f:s<9?e[8][15-s-1+1]=f:e[8][15-s-1]=f}e[a-8][8]=!0},r.prototype.setupVersionInfo=function(){if(this.version>=7)for(var t=this.matrix,e=this.matrixSize,i=lt(this.version),n=0;n<18;n++){var a=(i>>n&1)===1;t[n/3>>0][n%3+e-8-3]=a,t[n%3+e-8-3][n/3>>0]=a}},r.prototype.setupCodewords=function(t,e){for(var i=this.matrix,n=this.matrixSize,a=t.getLengthInBits(),s=St(e),f=0,u=n-1;u>=1;u-=2){u===6&&(u=5);for(var o=0;o<n;o++)for(var v=0;v<2;v++){var l=u-v,m=(u+1&2)===0,h=m?n-1-o:o;if(i[h][l]===null){var c=!1;f<a&&(c=t.getBit(f++));var g=s(l,h);g&&(c=!c),i[h][l]=c}}}},r.prototype.buildMatrix=function(t,e){for(var i=[],n=this.matrixSize,a=0;a<n;a++){i[a]=[];for(var s=0;s<n;s++)i[a][s]=null}this.matrix=i,this.setupFinderPattern(0,0),this.setupFinderPattern(n-7,0),this.setupFinderPattern(0,n-7),this.setupAlignmentPattern(),this.setupTimingPattern(),this.setupFormatInfo(e),this.setupVersionInfo(),this.setupCodewords(t,e)},r.prototype.make=function(){var t,e,i,n,a,s=this,f=s.chunks,u=s.errorCorrectionLevel;if(this.auto){for(var o=1;o<=40&&(t=K(o,u,this.encodingHint,f),i=t[0],n=t[1],a=t[2],!(i.getLengthInBits()<=a));o++);var v=i.getLengthInBits();if(v>a)throw new Error("data overflow: ".concat(v," > ").concat(a));this.version=o}else e=K(this.version,u,this.encodingHint,f),i=e[0],n=e[1],a=e[2];this.matrixSize=this.version*4+17;for(var l=[],m=Rt(i,n,a),h=-1,c=Number.MAX_VALUE,g=0;g<8;g++){this.buildMatrix(m,g),l.push(this.matrix);var b=yt(this);b<c&&(c=b,h=g)}return this.matrix=l[h],this},r.prototype.toDataURL=function(t,e){t===void 0&&(t=2),e===void 0&&(e=t*4),t=Math.max(1,t>>0),e=Math.max(0,e>>0);for(var i=this.matrixSize,n=t*i+e*2,a=e,s=n-e,f=new Ct(n,n),u=0;u<n;u++)for(var o=0;o<n;o++)if(a<=o&&o<s&&a<=u&&u<s){var v=(u-a)/t>>0,l=(o-a)/t>>0;f.setPixel(o,u,this.isDark(v,l)?0:1)}else f.setPixel(o,u,1);return f.toDataURL()},r.prototype.clear=function(){this.chunks=[]},r}();const zt={class:"page-container container"},Mt={class:"main"},Pt={class:"v-toolbar"},Ht=["src"],Ot=q({__name:"QrCodeGeneratorView",setup(r){const t=M(""),e=M("");return tt(t,i=>{try{const n=new Tt;n.setEncodingHint(!0),n.setErrorCorrectionLevel(y.H),n.write(i),n.make(),e.value=n.toDataURL(8)}catch{}}),(i,n)=>{const a=Y,s=J;return P(),H("div",zt,[O("div",Mt,[O("div",Pt,[x(a,{current:()=>i.$t("qrcode_generator")},null,8,["current"])]),x(S(j),{class:"panel-container"},{default:C(()=>[x(S(z),null,{default:C(()=>[x(s,{modelValue:t.value,"onUpdate:modelValue":n[0]||(n[0]=f=>t.value=f)},null,8,["modelValue"])]),_:1}),x(S(z),{class:"qrcode-panel"},{default:C(()=>[e.value?(P(),H("img",{key:0,src:e.value},null,8,Ht)):rt("",!0)]),_:1})]),_:1})])])}}});const Kt=et(Ot,[["__scopeId","data-v-bb05f6b7"]]);export{Kt as default};
