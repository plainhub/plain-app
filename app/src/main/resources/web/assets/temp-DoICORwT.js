import{A as e,E as t,F as n,P as r,Q as i,R as a,at as o,ct as s,dt as c,ft as l,g as u,it as d,k as f,nt as p,ot as m,pt as h,rt as g,st as _,tt as v}from"./vue.runtime.esm-bundler-DB7W0Wog.js";import{i as y,n as b}from"./prefs-CeTcgcp8.js";function x(){let e=new Map;function t(t,n){let r=e.get(t);r?r.add(n):e.set(t,new Set([n]))}function n(t,n){n?e.get(t)?.delete(n):e.delete(t)}function r(t,n){e.get(t)?.forEach(e=>e(n))}return{on:t,off:n,emit:r}}var S=x(),C=`__bound_client_id__`,w=`__window_id__`,T,E;function D(e){try{return sessionStorage.getItem(e)}catch{return null}}function O(e,t){try{t?sessionStorage.setItem(e,t):sessionStorage.removeItem(e)}catch{}}function k(){return b(`client_id`,``)}function A(){if(T!==void 0)return T??``;let e=D(C)??``;return T=e,e}function j(e){T=e,O(C,e)}function M(){T=``,O(C,``)}function N(){return A()||k()}function P(){return!A()}function F(e){if(!e)return!0;let t=k();return!t||e===t}function I(){if(E)return E;let e=D(w);return e||(e=`w_${Math.random().toString(36).slice(2,10)}_${Date.now().toString(36)}`,O(w,e)),E=e,e}function L(){try{let e=new URL(window.location.href),t=e.searchParams.get(`__cid`);if(!t)return;if(F(t)){e.searchParams.delete(`__cid`);let t=e.searchParams.toString(),n=e.pathname+(t?`?${t}`:``)+e.hash;window.history.replaceState({},``,n);return}j(t),e.searchParams.delete(`__cid`);let n=e.searchParams.toString(),r=e.pathname+(n?`?${n}`:``)+e.hash;window.history.replaceState({},``,r)}catch{}}var R=`device_sessions`;function ee(){try{let e=b(R,null);return!e||!Array.isArray(e.sessions)?{sessions:[]}:{sessions:e.sessions}}catch{return{sessions:[]}}}function z(){return N()}function B(){let{sessions:e}=ee();return e.find(e=>e.clientId===z())?.token??``}function V(){try{let e=b(R,null);if(!e)return;let t=z();if(t&&Array.isArray(e.sessions)){let n=e.sessions.find(e=>e.clientId===t);n&&(n.token=``),y(R,e)}M()}catch{}}function H(){let e=z();return e?`main_state:${e}`:`main_state`}var U=``;function te(e){U=e}function ne(){U=``}function re(){return U}var ie=``;function ae(){return ie}function oe(e){return`${de()}${e}`}function se(){return de()}function ce(){return window.location.host}function le(){return{"Content-Type":`multipart/form-data`,"c-id":b(`client_id`,``)}}function ue(){return`${window.location.protocol===`http:`?`ws`:`wss`}://${ce()}`}function de(){return`${window.location.protocol}//${ce()}`}function fe(e){return e instanceof Uint8Array||ArrayBuffer.isView(e)&&e.constructor.name===`Uint8Array`&&`BYTES_PER_ELEMENT`in e&&e.BYTES_PER_ELEMENT===1}function pe(e,t,n=``){let r=fe(e),i=e?.length,a=t!==void 0;if(!r||a&&i!==t){let o=n&&`"${n}" `,s=a?` of length ${t}`:``,c=r?`length=${i}`:`type=${typeof e}`,l=o+`expected Uint8Array`+s+`, got `+c;throw r?RangeError(l):TypeError(l)}return e}function me(e,t=!0){if(e.destroyed)throw Error(`Hash instance has been destroyed`);if(t&&e.finished)throw Error(`Hash#digest() has already been called`)}function he(e,t){pe(e,void 0,`digestInto() output`);let n=t.outputLen;if(e.length<n)throw RangeError(`"digestInto() output" expected to be of length >=`+n)}function ge(...e){for(let t=0;t<e.length;t++)e[t].fill(0)}function _e(e){return new DataView(e.buffer,e.byteOffset,e.byteLength)}var ve=typeof Uint8Array.from([]).toHex==`function`&&typeof Uint8Array.fromHex==`function`,ye=Array.from({length:256},(e,t)=>t.toString(16).padStart(2,`0`));function be(e){if(pe(e),ve)return e.toHex();let t=``;for(let n=0;n<e.length;n++)t+=ye[e[n]];return t}function xe(e,t={}){let n=(t,n)=>e(n).update(t).digest(),r=e(void 0);return n.outputLen=r.outputLen,n.blockLen=r.blockLen,n.canXOF=r.canXOF,n.create=t=>e(t),Object.assign(n,t),Object.freeze(n)}var Se=e=>({oid:Uint8Array.from([6,9,96,134,72,1,101,3,4,2,e])}),Ce=class{blockLen;outputLen;canXOF=!1;padOffset;isLE;buffer;view;finished=!1;length=0;pos=0;destroyed=!1;constructor(e,t,n,r){this.blockLen=e,this.outputLen=t,this.padOffset=n,this.isLE=r,this.buffer=new Uint8Array(e),this.view=_e(this.buffer)}update(e){me(this),pe(e);let{view:t,buffer:n,blockLen:r}=this,i=e.length;for(let a=0;a<i;){let o=Math.min(r-this.pos,i-a);if(o===r){let t=_e(e);for(;r<=i-a;a+=r)this.process(t,a);continue}n.set(e.subarray(a,a+o),this.pos),this.pos+=o,a+=o,this.pos===r&&(this.process(t,0),this.pos=0)}return this.length+=e.length,this.roundClean(),this}digestInto(e){me(this),he(e,this),this.finished=!0;let{buffer:t,view:n,blockLen:r,isLE:i}=this,{pos:a}=this;t[a++]=128,ge(this.buffer.subarray(a)),this.padOffset>r-a&&(this.process(n,0),a=0);for(let e=a;e<r;e++)t[e]=0;n.setBigUint64(r-8,BigInt(this.length*8),i),this.process(n,0);let o=_e(e),s=this.outputLen;if(s%4)throw Error(`_sha2: outputLen must be aligned to 32bit`);let c=s/4,l=this.get();if(c>l.length)throw Error(`_sha2: outputLen bigger than state`);for(let e=0;e<c;e++)o.setUint32(4*e,l[e],i)}digest(){let{buffer:e,outputLen:t}=this;this.digestInto(e);let n=e.slice(0,t);return this.destroy(),n}_cloneInto(e){e||=new this.constructor,e.set(...this.get());let{blockLen:t,buffer:n,length:r,finished:i,destroyed:a,pos:o}=this;return e.destroyed=a,e.finished=i,e.length=r,e.pos=o,r%t&&e.buffer.set(n),e}clone(){return this._cloneInto()}},W=Uint32Array.from([1779033703,4089235720,3144134277,2227873595,1013904242,4271175723,2773480762,1595750129,1359893119,2917565137,2600822924,725511199,528734635,4215389547,1541459225,327033209]),we=BigInt(2**32-1),Te=BigInt(32);function Ee(e,t=!1){return t?{h:Number(e&we),l:Number(e>>Te&we)}:{h:Number(e>>Te&we)|0,l:Number(e&we)|0}}function De(e,t=!1){let n=e.length,r=new Uint32Array(n),i=new Uint32Array(n);for(let a=0;a<n;a++){let{h:n,l:o}=Ee(e[a],t);[r[a],i[a]]=[n,o]}return[r,i]}var Oe=(e,t,n)=>e>>>n,ke=(e,t,n)=>e<<32-n|t>>>n,Ae=(e,t,n)=>e>>>n|t<<32-n,je=(e,t,n)=>e<<32-n|t>>>n,Me=(e,t,n)=>e<<64-n|t>>>n-32,Ne=(e,t,n)=>e>>>n-32|t<<64-n;function G(e,t,n,r){let i=(t>>>0)+(r>>>0);return{h:e+n+(i/2**32|0)|0,l:i|0}}var Pe=(e,t,n)=>(e>>>0)+(t>>>0)+(n>>>0),Fe=(e,t,n,r)=>t+n+r+(e/2**32|0)|0,Ie=(e,t,n,r)=>(e>>>0)+(t>>>0)+(n>>>0)+(r>>>0),Le=(e,t,n,r,i)=>t+n+r+i+(e/2**32|0)|0,Re=(e,t,n,r,i)=>(e>>>0)+(t>>>0)+(n>>>0)+(r>>>0)+(i>>>0),ze=(e,t,n,r,i,a)=>t+n+r+i+a+(e/2**32|0)|0,Be=De(`0x428a2f98d728ae22.0x7137449123ef65cd.0xb5c0fbcfec4d3b2f.0xe9b5dba58189dbbc.0x3956c25bf348b538.0x59f111f1b605d019.0x923f82a4af194f9b.0xab1c5ed5da6d8118.0xd807aa98a3030242.0x12835b0145706fbe.0x243185be4ee4b28c.0x550c7dc3d5ffb4e2.0x72be5d74f27b896f.0x80deb1fe3b1696b1.0x9bdc06a725c71235.0xc19bf174cf692694.0xe49b69c19ef14ad2.0xefbe4786384f25e3.0x0fc19dc68b8cd5b5.0x240ca1cc77ac9c65.0x2de92c6f592b0275.0x4a7484aa6ea6e483.0x5cb0a9dcbd41fbd4.0x76f988da831153b5.0x983e5152ee66dfab.0xa831c66d2db43210.0xb00327c898fb213f.0xbf597fc7beef0ee4.0xc6e00bf33da88fc2.0xd5a79147930aa725.0x06ca6351e003826f.0x142929670a0e6e70.0x27b70a8546d22ffc.0x2e1b21385c26c926.0x4d2c6dfc5ac42aed.0x53380d139d95b3df.0x650a73548baf63de.0x766a0abb3c77b2a8.0x81c2c92e47edaee6.0x92722c851482353b.0xa2bfe8a14cf10364.0xa81a664bbc423001.0xc24b8b70d0f89791.0xc76c51a30654be30.0xd192e819d6ef5218.0xd69906245565a910.0xf40e35855771202a.0x106aa07032bbd1b8.0x19a4c116b8d2d0c8.0x1e376c085141ab53.0x2748774cdf8eeb99.0x34b0bcb5e19b48a8.0x391c0cb3c5c95a63.0x4ed8aa4ae3418acb.0x5b9cca4f7763e373.0x682e6ff3d6b2b8a3.0x748f82ee5defb2fc.0x78a5636f43172f60.0x84c87814a1f0ab72.0x8cc702081a6439ec.0x90befffa23631e28.0xa4506cebde82bde9.0xbef9a3f7b2c67915.0xc67178f2e372532b.0xca273eceea26619c.0xd186b8c721c0c207.0xeada7dd6cde0eb1e.0xf57d4f7fee6ed178.0x06f067aa72176fba.0x0a637dc5a2c898a6.0x113f9804bef90dae.0x1b710b35131c471b.0x28db77f523047d84.0x32caab7b40c72493.0x3c9ebe0a15c9bebc.0x431d67c49c100d4c.0x4cc5d4becb3e42b6.0x597f299cfc657e2a.0x5fcb6fab3ad6faec.0x6c44198c4a475817`.split(`.`).map(e=>BigInt(e))),Ve=Be[0],He=Be[1],K=new Uint32Array(80),q=new Uint32Array(80),Ue=class extends Ce{constructor(e){super(128,e,16,!1)}get(){let{Ah:e,Al:t,Bh:n,Bl:r,Ch:i,Cl:a,Dh:o,Dl:s,Eh:c,El:l,Fh:u,Fl:d,Gh:f,Gl:p,Hh:m,Hl:h}=this;return[e,t,n,r,i,a,o,s,c,l,u,d,f,p,m,h]}set(e,t,n,r,i,a,o,s,c,l,u,d,f,p,m,h){this.Ah=e|0,this.Al=t|0,this.Bh=n|0,this.Bl=r|0,this.Ch=i|0,this.Cl=a|0,this.Dh=o|0,this.Dl=s|0,this.Eh=c|0,this.El=l|0,this.Fh=u|0,this.Fl=d|0,this.Gh=f|0,this.Gl=p|0,this.Hh=m|0,this.Hl=h|0}process(e,t){for(let n=0;n<16;n++,t+=4)K[n]=e.getUint32(t),q[n]=e.getUint32(t+=4);for(let e=16;e<80;e++){let t=K[e-15]|0,n=q[e-15]|0,r=Ae(t,n,1)^Ae(t,n,8)^Oe(t,n,7),i=je(t,n,1)^je(t,n,8)^ke(t,n,7),a=K[e-2]|0,o=q[e-2]|0,s=Ae(a,o,19)^Me(a,o,61)^Oe(a,o,6),c=Ie(i,je(a,o,19)^Ne(a,o,61)^ke(a,o,6),q[e-7],q[e-16]);K[e]=Le(c,r,s,K[e-7],K[e-16])|0,q[e]=c|0}let{Ah:n,Al:r,Bh:i,Bl:a,Ch:o,Cl:s,Dh:c,Dl:l,Eh:u,El:d,Fh:f,Fl:p,Gh:m,Gl:h,Hh:g,Hl:_}=this;for(let e=0;e<80;e++){let t=Ae(u,d,14)^Ae(u,d,18)^Me(u,d,41),v=je(u,d,14)^je(u,d,18)^Ne(u,d,41),y=u&f^~u&m,b=d&p^~d&h,x=Re(_,v,b,He[e],q[e]),S=ze(x,g,t,y,Ve[e],K[e]),C=x|0,w=Ae(n,r,28)^Me(n,r,34)^Me(n,r,39),T=je(n,r,28)^Ne(n,r,34)^Ne(n,r,39),E=n&i^n&o^i&o,D=r&a^r&s^a&s;g=m|0,_=h|0,m=f|0,h=p|0,f=u|0,p=d|0,{h:u,l:d}=G(c|0,l|0,S|0,C|0),c=o|0,l=s|0,o=i|0,s=a|0,i=n|0,a=r|0;let O=Pe(C,T,D);n=Fe(O,S,w,E),r=O|0}({h:n,l:r}=G(this.Ah|0,this.Al|0,n|0,r|0)),{h:i,l:a}=G(this.Bh|0,this.Bl|0,i|0,a|0),{h:o,l:s}=G(this.Ch|0,this.Cl|0,o|0,s|0),{h:c,l}=G(this.Dh|0,this.Dl|0,c|0,l|0),{h:u,l:d}=G(this.Eh|0,this.El|0,u|0,d|0),{h:f,l:p}=G(this.Fh|0,this.Fl|0,f|0,p|0),{h:m,l:h}=G(this.Gh|0,this.Gl|0,m|0,h|0),{h:g,l:_}=G(this.Hh|0,this.Hl|0,g|0,_|0),this.set(n,r,i,a,o,s,c,l,u,d,f,p,m,h,g,_)}roundClean(){ge(K,q)}destroy(){this.destroyed=!0,ge(this.buffer),this.set(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0)}},We=class extends Ue{Ah=W[0]|0;Al=W[1]|0;Bh=W[2]|0;Bl=W[3]|0;Ch=W[4]|0;Cl=W[5]|0;Dh=W[6]|0;Dl=W[7]|0;Eh=W[8]|0;El=W[9]|0;Fh=W[10]|0;Fl=W[11]|0;Gh=W[12]|0;Gl=W[13]|0;Hh=W[14]|0;Hl=W[15]|0;constructor(){super(64)}},Ge=xe(()=>new We,Se(3));function Ke(e){return be(Ge(new TextEncoder().encode(e)))}function qe(e){return e instanceof Uint8Array||ArrayBuffer.isView(e)&&e.constructor.name===`Uint8Array`}function Je(e){if(typeof e!=`boolean`)throw Error(`boolean expected, not ${e}`)}function Ye(e){if(!Number.isSafeInteger(e)||e<0)throw Error(`positive integer expected, got `+e)}function J(e,...t){if(!qe(e))throw Error(`Uint8Array expected`);if(t.length>0&&!t.includes(e.length))throw Error(`Uint8Array expected of length `+t+`, got length=`+e.length)}function Xe(e,t=!0){if(e.destroyed)throw Error(`Hash instance has been destroyed`);if(t&&e.finished)throw Error(`Hash#digest() has already been called`)}function Ze(e,t){J(e);let n=t.outputLen;if(e.length<n)throw Error(`digestInto() expects output buffer of length at least `+n)}function Y(e){return new Uint32Array(e.buffer,e.byteOffset,Math.floor(e.byteLength/4))}function Qe(...e){for(let t=0;t<e.length;t++)e[t].fill(0)}function $e(e){return new DataView(e.buffer,e.byteOffset,e.byteLength)}var et=new Uint8Array(new Uint32Array([287454020]).buffer)[0]===68;function tt(e){if(typeof e!=`string`)throw Error(`string expected`);return new Uint8Array(new TextEncoder().encode(e))}function nt(e){if(typeof e==`string`)e=tt(e);else if(qe(e))e=ut(e);else throw Error(`Uint8Array expected, got `+typeof e);return e}function rt(e,t){if(typeof t!=`object`||!t)throw Error(`options must be defined`);return Object.assign(e,t)}function it(e,t){if(e.length!==t.length)return!1;let n=0;for(let r=0;r<e.length;r++)n|=e[r]^t[r];return n===0}var at=(e,t)=>{function n(n,...r){if(J(n),!et)throw Error(`Non little-endian hardware is not yet supported`);if(e.nonceLength!==void 0){let t=r[0];if(!t)throw Error(`nonce / iv required`);e.varSizeNonce?J(t):J(t,e.nonceLength)}let i=e.tagLength;i&&r[1]!==void 0&&J(r[1]);let a=t(n,...r),o=(e,t)=>{if(t!==void 0){if(e!==2)throw Error(`cipher output not supported`);J(t)}},s=!1;return{encrypt(e,t){if(s)throw Error(`cannot encrypt() twice with same key + nonce`);return s=!0,J(e),o(a.encrypt.length,t),a.encrypt(e,t)},decrypt(e,t){if(J(e),i&&e.length<i)throw Error(`invalid ciphertext length: smaller than tagLength=`+i);return o(a.decrypt.length,t),a.decrypt(e,t)}}}return Object.assign(n,e),n};function ot(e,t,n=!0){if(t===void 0)return new Uint8Array(e);if(t.length!==e)throw Error(`invalid output length, expected `+e+`, got: `+t.length);if(n&&!lt(t))throw Error(`invalid output, must be aligned`);return t}function st(e,t,n,r){if(typeof e.setBigUint64==`function`)return e.setBigUint64(t,n,r);let i=BigInt(32),a=BigInt(4294967295),o=Number(n>>i&a),s=Number(n&a),c=r?4:0,l=r?0:4;e.setUint32(t+c,o,r),e.setUint32(t+l,s,r)}function ct(e,t,n){Je(n);let r=new Uint8Array(16),i=$e(r);return st(i,0,BigInt(t),n),st(i,8,BigInt(e),n),r}function lt(e){return e.byteOffset%4==0}function ut(e){return Uint8Array.from(e)}var dt=e=>Uint8Array.from(e.split(``).map(e=>e.charCodeAt(0))),ft=dt(`expand 16-byte k`),pt=dt(`expand 32-byte k`),mt=Y(ft),ht=Y(pt);function X(e,t){return e<<t|e>>>32-t}function gt(e){return e.byteOffset%4==0}var _t=64,vt=16,yt=2**32-1,bt=new Uint32Array;function xt(e,t,n,r,i,a,o,s){let c=i.length,l=new Uint8Array(_t),u=Y(l),d=gt(i)&&gt(a),f=d?Y(i):bt,p=d?Y(a):bt;for(let m=0;m<c;o++){if(e(t,n,r,u,o,s),o>=yt)throw Error(`arx: counter overflow`);let h=Math.min(_t,c-m);if(d&&h===_t){let e=m/4;if(m%4!=0)throw Error(`arx: invalid block position`);for(let t=0,n;t<vt;t++)n=e+t,p[n]=f[n]^u[t];m+=_t;continue}for(let e=0,t;e<h;e++)t=m+e,a[t]=i[t]^l[e];m+=h}}function St(e,t){let{allowShortKeys:n,extendNonceFn:r,counterLength:i,counterRight:a,rounds:o}=rt({allowShortKeys:!1,counterLength:8,counterRight:!1,rounds:20},t);if(typeof e!=`function`)throw Error(`core must be a function`);return Ye(i),Ye(o),Je(a),Je(n),(t,s,c,l,u=0)=>{J(t),J(s),J(c);let d=c.length;if(l===void 0&&(l=new Uint8Array(d)),J(l),Ye(u),u<0||u>=yt)throw Error(`arx: counter overflow`);if(l.length<d)throw Error(`arx: output (${l.length}) is shorter than data (${d})`);let f=[],p=t.length,m,h;if(p===32)f.push(m=ut(t)),h=ht;else if(p===16&&n)m=new Uint8Array(32),m.set(t),m.set(t,16),h=mt,f.push(m);else throw Error(`arx: invalid 32-byte key, got length=${p}`);gt(s)||f.push(s=ut(s));let g=Y(m);if(r){if(s.length!==24)throw Error(`arx: extended nonce must be 24 bytes`);r(h,g,Y(s.subarray(0,16)),g),s=s.subarray(16)}let _=16-i;if(_!==s.length)throw Error(`arx: nonce must be ${_} or 16 bytes`);if(_!==12){let e=new Uint8Array(12);e.set(s,a?0:12-s.length),s=e,f.push(s)}let v=Y(s);return xt(e,h,g,v,c,l,u,o),Qe(...f),l}}var Z=(e,t)=>e[t++]&255|(e[t++]&255)<<8,Ct=class{constructor(e){this.blockLen=16,this.outputLen=16,this.buffer=new Uint8Array(16),this.r=new Uint16Array(10),this.h=new Uint16Array(10),this.pad=new Uint16Array(8),this.pos=0,this.finished=!1,e=nt(e),J(e,32);let t=Z(e,0),n=Z(e,2),r=Z(e,4),i=Z(e,6),a=Z(e,8),o=Z(e,10),s=Z(e,12),c=Z(e,14);this.r[0]=t&8191,this.r[1]=(t>>>13|n<<3)&8191,this.r[2]=(n>>>10|r<<6)&7939,this.r[3]=(r>>>7|i<<9)&8191,this.r[4]=(i>>>4|a<<12)&255,this.r[5]=a>>>1&8190,this.r[6]=(a>>>14|o<<2)&8191,this.r[7]=(o>>>11|s<<5)&8065,this.r[8]=(s>>>8|c<<8)&8191,this.r[9]=c>>>5&127;for(let t=0;t<8;t++)this.pad[t]=Z(e,16+2*t)}process(e,t,n=!1){let r=n?0:2048,{h:i,r:a}=this,o=a[0],s=a[1],c=a[2],l=a[3],u=a[4],d=a[5],f=a[6],p=a[7],m=a[8],h=a[9],g=Z(e,t+0),_=Z(e,t+2),v=Z(e,t+4),y=Z(e,t+6),b=Z(e,t+8),x=Z(e,t+10),S=Z(e,t+12),C=Z(e,t+14),w=i[0]+(g&8191),T=i[1]+((g>>>13|_<<3)&8191),E=i[2]+((_>>>10|v<<6)&8191),D=i[3]+((v>>>7|y<<9)&8191),O=i[4]+((y>>>4|b<<12)&8191),k=i[5]+(b>>>1&8191),A=i[6]+((b>>>14|x<<2)&8191),j=i[7]+((x>>>11|S<<5)&8191),M=i[8]+((S>>>8|C<<8)&8191),N=i[9]+(C>>>5|r),P=0,F=P+w*o+5*h*T+5*m*E+5*p*D+5*f*O;P=F>>>13,F&=8191,F+=5*d*k+5*u*A+5*l*j+5*c*M+5*s*N,P+=F>>>13,F&=8191;let I=P+w*s+T*o+5*h*E+5*m*D+5*p*O;P=I>>>13,I&=8191,I+=5*f*k+5*d*A+5*u*j+5*l*M+5*c*N,P+=I>>>13,I&=8191;let L=P+w*c+T*s+E*o+5*h*D+5*m*O;P=L>>>13,L&=8191,L+=5*p*k+5*f*A+5*d*j+5*u*M+5*l*N,P+=L>>>13,L&=8191;let R=P+w*l+T*c+E*s+D*o+5*h*O;P=R>>>13,R&=8191,R+=5*m*k+5*p*A+5*f*j+5*d*M+5*u*N,P+=R>>>13,R&=8191;let ee=P+w*u+T*l+E*c+D*s+O*o;P=ee>>>13,ee&=8191,ee+=5*h*k+5*m*A+5*p*j+5*f*M+5*d*N,P+=ee>>>13,ee&=8191;let z=P+w*d+T*u+E*l+D*c+O*s;P=z>>>13,z&=8191,z+=k*o+5*h*A+5*m*j+5*p*M+5*f*N,P+=z>>>13,z&=8191;let B=P+w*f+T*d+E*u+D*l+O*c;P=B>>>13,B&=8191,B+=k*s+A*o+5*h*j+5*m*M+5*p*N,P+=B>>>13,B&=8191;let V=P+w*p+T*f+E*d+D*u+O*l;P=V>>>13,V&=8191,V+=k*c+A*s+j*o+5*h*M+5*m*N,P+=V>>>13,V&=8191;let H=P+w*m+T*p+E*f+D*d+O*u;P=H>>>13,H&=8191,H+=k*l+A*c+j*s+M*o+5*h*N,P+=H>>>13,H&=8191;let U=P+w*h+T*m+E*p+D*f+O*d;P=U>>>13,U&=8191,U+=k*u+A*l+j*c+M*s+N*o,P+=U>>>13,U&=8191,P=(P<<2)+P|0,P=P+F|0,F=P&8191,P>>>=13,I+=P,i[0]=F,i[1]=I,i[2]=L,i[3]=R,i[4]=ee,i[5]=z,i[6]=B,i[7]=V,i[8]=H,i[9]=U}finalize(){let{h:e,pad:t}=this,n=new Uint16Array(10),r=e[1]>>>13;e[1]&=8191;for(let t=2;t<10;t++)e[t]+=r,r=e[t]>>>13,e[t]&=8191;e[0]+=r*5,r=e[0]>>>13,e[0]&=8191,e[1]+=r,r=e[1]>>>13,e[1]&=8191,e[2]+=r,n[0]=e[0]+5,r=n[0]>>>13,n[0]&=8191;for(let t=1;t<10;t++)n[t]=e[t]+r,r=n[t]>>>13,n[t]&=8191;n[9]-=8192;let i=(r^1)-1;for(let e=0;e<10;e++)n[e]&=i;i=~i;for(let t=0;t<10;t++)e[t]=e[t]&i|n[t];e[0]=(e[0]|e[1]<<13)&65535,e[1]=(e[1]>>>3|e[2]<<10)&65535,e[2]=(e[2]>>>6|e[3]<<7)&65535,e[3]=(e[3]>>>9|e[4]<<4)&65535,e[4]=(e[4]>>>12|e[5]<<1|e[6]<<14)&65535,e[5]=(e[6]>>>2|e[7]<<11)&65535,e[6]=(e[7]>>>5|e[8]<<8)&65535,e[7]=(e[8]>>>8|e[9]<<5)&65535;let a=e[0]+t[0];e[0]=a&65535;for(let n=1;n<8;n++)a=(e[n]+t[n]|0)+(a>>>16)|0,e[n]=a&65535;Qe(n)}update(e){Xe(this),e=nt(e),J(e);let{buffer:t,blockLen:n}=this,r=e.length;for(let i=0;i<r;){let a=Math.min(n-this.pos,r-i);if(a===n){for(;n<=r-i;i+=n)this.process(e,i);continue}t.set(e.subarray(i,i+a),this.pos),this.pos+=a,i+=a,this.pos===n&&(this.process(t,0,!1),this.pos=0)}return this}destroy(){Qe(this.h,this.r,this.buffer,this.pad)}digestInto(e){Xe(this),Ze(e,this),this.finished=!0;let{buffer:t,h:n}=this,{pos:r}=this;if(r){for(t[r++]=1;r<16;r++)t[r]=0;this.process(t,0,!0)}this.finalize();let i=0;for(let t=0;t<8;t++)e[i++]=n[t]>>>0,e[i++]=n[t]>>>8;return e}digest(){let{buffer:e,outputLen:t}=this;this.digestInto(e);let n=e.slice(0,t);return this.destroy(),n}};function wt(e){let t=(t,n)=>e(n).update(nt(t)).digest(),n=e(new Uint8Array(32));return t.outputLen=n.outputLen,t.blockLen=n.blockLen,t.create=t=>e(t),t}var Tt=wt(e=>new Ct(e));function Et(e,t,n,r,i,a=20){let o=e[0],s=e[1],c=e[2],l=e[3],u=t[0],d=t[1],f=t[2],p=t[3],m=t[4],h=t[5],g=t[6],_=t[7],v=i,y=n[0],b=n[1],x=n[2],S=o,C=s,w=c,T=l,E=u,D=d,O=f,k=p,A=m,j=h,M=g,N=_,P=v,F=y,I=b,L=x;for(let e=0;e<a;e+=2)S=S+E|0,P=X(P^S,16),A=A+P|0,E=X(E^A,12),S=S+E|0,P=X(P^S,8),A=A+P|0,E=X(E^A,7),C=C+D|0,F=X(F^C,16),j=j+F|0,D=X(D^j,12),C=C+D|0,F=X(F^C,8),j=j+F|0,D=X(D^j,7),w=w+O|0,I=X(I^w,16),M=M+I|0,O=X(O^M,12),w=w+O|0,I=X(I^w,8),M=M+I|0,O=X(O^M,7),T=T+k|0,L=X(L^T,16),N=N+L|0,k=X(k^N,12),T=T+k|0,L=X(L^T,8),N=N+L|0,k=X(k^N,7),S=S+D|0,L=X(L^S,16),M=M+L|0,D=X(D^M,12),S=S+D|0,L=X(L^S,8),M=M+L|0,D=X(D^M,7),C=C+O|0,P=X(P^C,16),N=N+P|0,O=X(O^N,12),C=C+O|0,P=X(P^C,8),N=N+P|0,O=X(O^N,7),w=w+k|0,F=X(F^w,16),A=A+F|0,k=X(k^A,12),w=w+k|0,F=X(F^w,8),A=A+F|0,k=X(k^A,7),T=T+E|0,I=X(I^T,16),j=j+I|0,E=X(E^j,12),T=T+E|0,I=X(I^T,8),j=j+I|0,E=X(E^j,7);let R=0;r[R++]=o+S|0,r[R++]=s+C|0,r[R++]=c+w|0,r[R++]=l+T|0,r[R++]=u+E|0,r[R++]=d+D|0,r[R++]=f+O|0,r[R++]=p+k|0,r[R++]=m+A|0,r[R++]=h+j|0,r[R++]=g+M|0,r[R++]=_+N|0,r[R++]=v+P|0,r[R++]=y+F|0,r[R++]=b+I|0,r[R++]=x+L|0}function Dt(e,t,n,r){let i=e[0],a=e[1],o=e[2],s=e[3],c=t[0],l=t[1],u=t[2],d=t[3],f=t[4],p=t[5],m=t[6],h=t[7],g=n[0],_=n[1],v=n[2],y=n[3];for(let e=0;e<20;e+=2)i=i+c|0,g=X(g^i,16),f=f+g|0,c=X(c^f,12),i=i+c|0,g=X(g^i,8),f=f+g|0,c=X(c^f,7),a=a+l|0,_=X(_^a,16),p=p+_|0,l=X(l^p,12),a=a+l|0,_=X(_^a,8),p=p+_|0,l=X(l^p,7),o=o+u|0,v=X(v^o,16),m=m+v|0,u=X(u^m,12),o=o+u|0,v=X(v^o,8),m=m+v|0,u=X(u^m,7),s=s+d|0,y=X(y^s,16),h=h+y|0,d=X(d^h,12),s=s+d|0,y=X(y^s,8),h=h+y|0,d=X(d^h,7),i=i+l|0,y=X(y^i,16),m=m+y|0,l=X(l^m,12),i=i+l|0,y=X(y^i,8),m=m+y|0,l=X(l^m,7),a=a+u|0,g=X(g^a,16),h=h+g|0,u=X(u^h,12),a=a+u|0,g=X(g^a,8),h=h+g|0,u=X(u^h,7),o=o+d|0,_=X(_^o,16),f=f+_|0,d=X(d^f,12),o=o+d|0,_=X(_^o,8),f=f+_|0,d=X(d^f,7),s=s+c|0,v=X(v^s,16),p=p+v|0,c=X(c^p,12),s=s+c|0,v=X(v^s,8),p=p+v|0,c=X(c^p,7);let b=0;r[b++]=i,r[b++]=a,r[b++]=o,r[b++]=s,r[b++]=g,r[b++]=_,r[b++]=v,r[b++]=y}var Ot=St(Et,{counterRight:!1,counterLength:8,extendNonceFn:Dt,allowShortKeys:!1}),kt=new Uint8Array(16),At=(e,t)=>{e.update(t);let n=t.length%16;n&&e.update(kt.subarray(n))},jt=new Uint8Array(32);function Mt(e,t,n,r,i){let a=e(t,n,jt),o=Tt.create(a);i&&At(o,i),At(o,r);let s=ct(r.length,i?i.length:0,!0);o.update(s);let c=o.digest();return Qe(a,s),c}var Nt=at({blockSize:64,nonceLength:24,tagLength:16},(e=>(t,n,r)=>({encrypt(i,a){let o=i.length;a=ot(o+16,a,!1),a.set(i);let s=a.subarray(0,-16);e(t,n,s,s,1);let c=Mt(e,t,n,s,r);return a.set(c,o),Qe(c),a},decrypt(i,a){a=ot(i.length-16,a,!1);let o=i.subarray(0,-16),s=i.subarray(-16),c=Mt(e,t,n,o,r);if(!it(s,c))throw Error(`invalid tag`);return a.set(i.subarray(0,-16)),e(t,n,a,a,1),Qe(c),a}}))(Ot)),Pt=typeof globalThis==`object`&&`crypto`in globalThis?globalThis.crypto:void 0;function Ft(e=32){if(Pt&&typeof Pt.getRandomValues==`function`)return Pt.getRandomValues(new Uint8Array(e));if(Pt&&typeof Pt.randomBytes==`function`)return Uint8Array.from(Pt.randomBytes(e));throw Error(`crypto.getRandomValues must be defined`)}function It(e){return Ke(e)}function Lt(e){let t=e.substring(0,32);return new Uint8Array(t.split(``).map(e=>e.charCodeAt(0)))}function Rt(e,t){let n=Ft(24),r=new Uint8Array(32);r.set(e.slice(0,32));let i=new TextEncoder().encode(t),a=Nt(r,n).encrypt(i),o=new Uint8Array(n.length+a.length);return o.set(n,0),o.set(a,n.length),o}function zt(e,t){let n=t.slice(0,24),r=t.slice(24),i=new Uint8Array(32);i.set(e.slice(0,32));let a=Nt(i,n).decrypt(r);return new TextDecoder().decode(a)}function Bt(e){return new Uint8Array(e)}function Vt(e){return e}function Ht(e){return btoa(String.fromCharCode(...e))}var Ut=`123456789abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ`,Wt=()=>{let e=crypto.getRandomValues(new Uint8Array(16));e[6]=e[6]&15|64,e[8]=e[8]&63|128;let t=Kt(e).toLowerCase();return`${t.substring(0,8)}-${t.substring(8,12)}-${t.substring(12,16)}-${t.substring(16,20)}-${t.substring(20)}`},Gt=()=>{let e=$t(`0123456789abcdef`,Ut,Wt().toLowerCase().replace(/-/g,``)),t=Math.ceil(Math.log(2**128)/Math.log(58));return e.padStart(t,Ut[0])},Kt=e=>{let t=``;for(let n=0;n<e.length;n++)t+=e[n].toString(16).padStart(2,`0`);return t},qt=e=>Kt(new Uint8Array(e)),Jt=e=>Yt(atob(e)),Yt=e=>{let t=new Uint8Array(e.length);for(let n=0;n<e.length;n++)t[n]=e.charCodeAt(n);return t},Xt=e=>/[\u3400-\u9FBF]/.test(e);function Zt(e){return e?btoa(encodeURIComponent(e)):``}function Qt(e){try{return decodeURIComponent(atob(e))}catch{return e}}function $t(e,t,n){let r=[],i=e.length,a=t.length,o,s,c,l=n.length,u=``;if(e===t)return n;for(o=0;o<l;o++)r[o]=e.indexOf(n[o]);do{for(s=0,c=0,o=0;o<l;o++)s=s*i+r[o],s>=a?(r[c++]=parseInt((s/a).toString(),10),s%=a):c>0&&(r[c++]=0);l=c,u=t.slice(s,s+1).concat(u)}while(c!==0);return u}var en=/(\b(((https?|ftp):\/\/)|www.)[A-Z0-9+&@#\/%?=~_|!:,.;-]*[-A-Z0-9+&@#\/%=~_|])/gim,tn=/(\w+@[a-zA-Z_]+?\.[a-zA-Z]{2,6})/gim;function nn(e){let t=[],n=e=>`\x00P${e}\x00`,r=e.replace(en,e=>{let r=t.length,i=rn(e);return t.push(`<a href="${i}" target="_blank">${i}</a>`),n(r)});return r=r.replace(tn,e=>{let r=t.length;return t.push(`<a href="mailto:${e}">${e}</a>`),n(r)}),r=rn(r),r=r.replace(/\x00P(\d+)\x00/g,(e,n)=>t[Number(n)]),r.replace(/\n\r?/g,`<br />`)}function rn(e){return e.replace(/[\u00A0-\u9999<>&'"]/gim,function(e){return`&#`+e.charCodeAt(0)+`;`})}function an(e){return e.replace(/!\[.*?\]\(.*?\)|!\[.*?\]\[.*?\]|<img.*?>/gi,`🖼`).replace(`
`,``).replace(/^\s*/,``)}function on(e){let t=e.toLowerCase();return t.startsWith(`https://`)||t.startsWith(`http://`)||t.startsWith(`blob:`)}function sn(e,t=``){return on(e)?e:oe(`/fs?id=${encodeURIComponent(e)}${t}`)}function cn(e,t){return!t||!e?``:sn(_n(e,t))}var ln=new Set;function un(e,t){if(ln.has(e))return;ln.add(e),setTimeout(()=>ln.delete(e),1e3);let n=document.createElement(`a`);typeof n.download==`string`?(n.href=e,n.download=t,document.body.appendChild(n),n.click(),document.body.removeChild(n)):window.open(e)}function dn(e,t,n){let r=new Blob([e],{type:t});un(URL.createObjectURL(r),n)}function fn(e){return e?e.substring(e.lastIndexOf(`/`)+1):``}function pn(e){return Uint8Array.from(atob(e),e=>e.charCodeAt(0))}function mn(e,t){return e?Ht(Rt(e,t)):``}function hn(e,t,n,r=``){if(!e||!t?.ip||!t?.port||!n)return``;let i=mn(e,`https://${t.ip}:${t.port}/fs?id=${encodeURIComponent(n)}${r}`);return`${de()}/proxyfs?id=${encodeURIComponent(i)}`}function gn(e,t){if(t.startsWith(`app://`))return e+`/`+t.replace(`app://`,``);if(t.startsWith(`fid:`)){let n=t.replace(`fid:`,``);return`${e}/${n.substring(0,2)}/${n.substring(2,4)}/${n}`}return t}function _n(e,t,n=``){if(!t||!e)return``;let r=t.toLowerCase();if(r.startsWith(`https://`)||r.startsWith(`http://`))return t;let i=window.fileIdMap||new Map;if(i.has(t))return i.get(t)??``;let a=Ht(Rt(e,n?JSON.stringify({path:t,mediaId:n}):t));return i.set(t,a),a}function vn(e){let t=e.lastIndexOf(`.`);return t===-1||e.lastIndexOf(`/`)>t?``:e.substring(t+1).toLowerCase()}var yn=[`chat`],bn=[`home`,`developer`,...yn];function xn(e){return yn.includes(e)}function Sn(e){return typeof e==`string`&&bn.includes(e)}function Cn(){return!1}var wn=typeof window<`u`,Tn,En=e=>Tn=e,Dn=Symbol();function On(e){return e&&typeof e==`object`&&Object.prototype.toString.call(e)===`[object Object]`&&typeof e.toJSON!=`function`}var kn;(function(e){e.direct=`direct`,e.patchObject=`patch object`,e.patchFunction=`patch function`})(kn||={});var An=typeof window==`object`&&window.window===window?window:typeof self==`object`&&self.self===self?self:typeof global==`object`&&global.global===global?global:typeof globalThis==`object`?globalThis:{HTMLElement:null};function jn(e,{autoBom:t=!1}={}){return t&&/^\s*(?:text\/\S*|application\/xml|\S*\/\S*\+xml)\s*;.*charset\s*=\s*utf-8/i.test(e.type)?new Blob([`﻿`,e],{type:e.type}):e}function Mn(e,t,n){let r=new XMLHttpRequest;r.open(`GET`,e),r.responseType=`blob`,r.onload=function(){Ln(r.response,t,n)},r.onerror=function(){console.error(`could not download file`)},r.send()}function Nn(e){let t=new XMLHttpRequest;t.open(`HEAD`,e,!1);try{t.send()}catch{}return t.status>=200&&t.status<=299}function Pn(e){try{e.dispatchEvent(new MouseEvent(`click`))}catch{let t=new MouseEvent(`click`,{bubbles:!0,cancelable:!0,view:window,detail:0,screenX:80,screenY:20,clientX:80,clientY:20,ctrlKey:!1,altKey:!1,shiftKey:!1,metaKey:!1,button:0,relatedTarget:null});e.dispatchEvent(t)}}var Fn=typeof navigator==`object`?navigator:{userAgent:``},In=/Macintosh/.test(Fn.userAgent)&&/AppleWebKit/.test(Fn.userAgent)&&!/Safari/.test(Fn.userAgent),Ln=wn?typeof HTMLAnchorElement<`u`&&`download`in HTMLAnchorElement.prototype&&!In?Rn:`msSaveOrOpenBlob`in Fn?zn:Bn:()=>{};function Rn(e,t=`download`,n){let r=document.createElement(`a`);r.download=t,r.rel=`noopener`,typeof e==`string`?(r.href=e,r.origin===location.origin?Pn(r):Nn(r.href)?Mn(e,t,n):(r.target=`_blank`,Pn(r))):(r.href=URL.createObjectURL(e),setTimeout(function(){URL.revokeObjectURL(r.href)},4e4),setTimeout(function(){Pn(r)},0))}function zn(e,t=`download`,n){if(typeof e==`string`)if(Nn(e))Mn(e,t,n);else{let t=document.createElement(`a`);t.href=e,t.target=`_blank`,setTimeout(function(){Pn(t)})}else navigator.msSaveOrOpenBlob(jn(e,n),t)}function Bn(e,t,n,r){if(r||=open(``,`_blank`),r&&(r.document.title=r.document.body.innerText=`downloading...`),typeof e==`string`)return Mn(e,t,n);let i=e.type===`application/octet-stream`,a=/constructor/i.test(String(An.HTMLElement))||`safari`in An,o=/CriOS\/[\d]+/.test(navigator.userAgent);if((o||i&&a||In)&&typeof FileReader<`u`){let t=new FileReader;t.onloadend=function(){let e=t.result;if(typeof e!=`string`)throw r=null,Error(`Wrong reader.result type`);e=o?e:e.replace(/^data:[^;]*;/,`data:attachment/file;`),r?r.location.href=e:location.assign(e),r=null},t.readAsDataURL(e)}else{let t=URL.createObjectURL(e);r?r.location.assign(t):location.href=t,r=null,setTimeout(function(){URL.revokeObjectURL(t)},4e4)}}var{assign:Vn}=Object;function Hn(){let e=v(!0),t=e.run(()=>s({})),n=[],r=[],i=o({install(e){En(i),i._a=e,e.provide(Dn,i),e.config.globalProperties.$pinia=i,r.forEach(e=>n.push(e)),r=[]},use(e){return this._a?n.push(e):r.push(e),this},_p:n,_a:null,_e:e,_s:new Map,state:t});return i}var Un=()=>{};function Wn(e,t,n,r=Un){e.add(t);let i=()=>{e.delete(t)&&r()};return!n&&p()&&m(i),i}function Gn(e,...t){e.forEach(e=>{e(...t)})}var Kn=e=>e(),qn=Symbol(),Jn=Symbol();function Yn(e,t){e instanceof Map&&t instanceof Map?t.forEach((t,n)=>e.set(n,t)):e instanceof Set&&t instanceof Set&&t.forEach(e.add,e);for(let n in t){if(!t.hasOwnProperty(n))continue;let r=t[n],i=e[n];On(i)&&On(r)&&e.hasOwnProperty(n)&&!d(r)&&!g(r)?e[n]=Yn(i,r):e[n]=r}return e}var Xn=Symbol();function Zn(e){return!On(e)||!Object.prototype.hasOwnProperty.call(e,Xn)}var{assign:Q}=Object;function Qn(e){return!!(d(e)&&e.effect)}function $n(e,t,n,r){let{state:i,actions:a,getters:s}=t,c=n.state.value[e],l;function d(){return c||(n.state.value[e]=i?i():{}),Q(h(n.state.value[e]),a,Object.keys(s||{}).reduce((t,r)=>(t[r]=o(u(()=>{En(n);let t=n._s.get(e);return s[r].call(t,t)})),t),{}))}return l=er(e,d,t,n,r,!0),l}function er(e,t,n={},a,o,l){let u,f=Q({actions:{}},n),p={deep:!0},m,h,y=new Set,b=new Set,x=a.state.value[e];!l&&!x&&(a.state.value[e]={}),s({});let S;function C(t){let n;m=h=!1,typeof t==`function`?(t(a.state.value[e]),n={type:kn.patchFunction,storeId:e,events:void 0}):(Yn(a.state.value[e],t),n={type:kn.patchObject,payload:t,storeId:e,events:void 0});let i=S=Symbol();r().then(()=>{S===i&&(m=!0)}),h=!0,Gn(y,n,a.state.value[e])}let w=l?function(){let{state:e}=n,t=e?e():{};this.$patch(e=>{Q(e,t)})}:Un;function T(){u.stop(),y.clear(),b.clear(),a._s.delete(e)}let E=(t,n=``)=>{if(qn in t)return t[Jn]=n,t;let r=function(){En(a);let n=Array.from(arguments),i=new Set,o=new Set;function s(e){i.add(e)}function c(e){o.add(e)}Gn(b,{args:n,name:r[Jn],store:D,after:s,onError:c});let l;try{l=t.apply(this&&this.$id===e?this:D,n)}catch(e){throw Gn(o,e),e}return l instanceof Promise?l.then(e=>(Gn(i,e),e)).catch(e=>(Gn(o,e),Promise.reject(e))):(Gn(i,l),l)};return r[qn]=!0,r[Jn]=n,r},D=_({_p:a,$id:e,$onAction:Wn.bind(null,b),$patch:C,$reset:w,$subscribe(t,n={}){let r=Wn(y,t,n.detached,()=>o()),o=u.run(()=>i(()=>a.state.value[e],r=>{(n.flush===`sync`?h:m)&&t({storeId:e,type:kn.direct,events:void 0},r)},Q({},p,n)));return r},$dispose:T});a._s.set(e,D);let O=(a._a&&a._a.runWithContext||Kn)(()=>a._e.run(()=>(u=v()).run(()=>t({action:E}))));for(let t in O){let n=O[t];d(n)&&!Qn(n)||g(n)?l||(x&&Zn(n)&&(d(n)?n.value=x[t]:Yn(n,x[t])),a.state.value[e][t]=n):typeof n==`function`&&(O[t]=E(n,t),f.actions[t]=n)}return Q(D,O),Q(c(D),O),Object.defineProperty(D,"$state",{get:()=>a.state.value[e],set:e=>{C(t=>{Q(t,e)})}}),a._p.forEach(e=>{Q(D,u.run(()=>e({store:D,app:a._a,pinia:a,options:f})))}),x&&l&&n.hydrate&&n.hydrate(D.$state,x),m=!0,h=!0,D}function tr(t,n,r){let i,a=typeof n==`function`;i=a?r:n;function o(r,o){let s=f();return r||=s?e(Dn,null):null,r&&En(r),r=Tn,r._s.has(t)||(a?er(t,n,i,r):$n(t,i,r)),r._s.get(t)}return o.$id=t,o}function nr(e){let t=c(e),n={};for(let r in t){let i=t[r];i.effect?n[r]=u({get:()=>e[r],set(t){e[r]=t}}):(d(i)||g(i))&&(n[r]=l(e,r))}return n}var rr=window.__SERVER_TIME__?window.__SERVER_TIME__-Date.now():0;function ir(){return console.log(`Server time offset: ${rr} ms`),Date.now()+rr}function ar(){let e=new Uint8Array(16);return crypto.getRandomValues(e),Kt(e)}function or(e){return`${ir()}|${ar()}|${e}`}var sr=3e4,cr=new Map;async function lr(e,t){let n=JSON.stringify({query:e,variables:t}),r=cr.get(n);if(r)return r;let i=ur(e,t);cr.set(n,i);try{return await i}finally{cr.delete(n)}}async function ur(e,t){let n=`${de()}/graphql`,r=pn(P()?ae():B()),i=JSON.stringify({query:e,variables:t});console.info(`[request] ${i}`);let a=performance.now(),o=Vt(Rt(r,or(i))),s=performance.now(),c=new AbortController,l=setTimeout(()=>c.abort(),sr);try{let e=await fetch(n,{method:`POST`,headers:{...le()},body:o,signal:c.signal});if(e.status===401)throw P()||(V(),window.location.reload()),new dr(`unauthorized`,401);if(e.status===403)throw new dr(`web_access_disabled`,403);let t=await e.arrayBuffer(),i=performance.now(),l=zt(r,Bt(t)),u=performance.now();return console.info(`[response] ${l}`),console.info(`[time] encrypt: ${s-a}ms, api: ${i-s}ms, decrypt: ${u-i}ms`),JSON.parse(l)}catch(e){throw e instanceof dr?e:e.name===`AbortError`?new dr(`connection_timeout`):new dr(e.message||`network_error`)}finally{clearTimeout(l)}}var dr=class extends Error{status;constructor(e,t){super(e),this.status=t,this.name=`GqlError`}},fr=`
  fragment TagFragment on Tag {
    id
    name
    count
  }
`,$=`
  fragment TagSubFragment on Tag {
    id
    name
  }
`,pr=`
  fragment PlaylistAudioFragment on PlaylistAudio {
    title
    artist
    path
    duration
  }
`,mr=`
  fragment AppFragment on App {
    clientId
    usbConnected
    urlToken
    httpPort
    httpsPort
    appDir
    deviceName
    battery
    appVersion
    osVersion
    channel
    permissions
    audios {
      ...PlaylistAudioFragment
    }
    audioCurrent
    audioMode
    sdcardPath
    usbDiskPaths
    internalStoragePath
    downloadsDir
    developerMode
    debug
    favoriteFolders {
      rootPath
      fullPath
      alias
    }
  }
  ${pr}
`,hr=`
  fragment ChatItemFragment on ChatItem {
    id
    fromId
    toId
    channelId
    createdAt
    content
    status
    statusData
    data {
      ... on MessageImages {
        ids
      }
      ... on MessageFiles {
        ids
      }
      ... on MessageText {
        ids
      }
    }
  }
`,gr=`
  fragment MessageFragment on Message {
    id
    body
    address
    serviceCenter
    date
    type
    threadId
    subscriptionId
    isMms
    attachments {
      path
      contentType
      name
    }
    tags {
      ...TagSubFragment
    }
  }
  ${$}
`,_r=`
  fragment MessageConversationFragment on MessageConversation {
    id
    address
    snippet
    date
    messageCount
    read
  }
`,vr=`
  fragment ContactFragment on Contact {
    id
    suffix
    prefix
    firstName
    middleName
    lastName
    updatedAt
    notes
    source
    thumbnailId
    starred
    phoneNumbers {
      label
      value
      type
      normalizedNumber
    }
    addresses {
      ...ContentItemFagment
    }
    emails {
      ...ContentItemFagment
    }
    websites {
      ...ContentItemFagment
    }
    events {
      ...ContentItemFagment
    }
    ims {
      ...ContentItemFagment
    }
    tags {
      ...TagSubFragment
    }
  }
  ${$}
  fragment ContentItemFagment on ContentItem {
    label
    value
    type
  }
`,yr=`
  fragment CallFragment on Call {
    id
    name
    number
    duration
    accountId
    startedAt
    photoId
    type
    geo {
      isp
      city
      province
    }
    tags {
      ...TagSubFragment
    }
  }
  ${$}
`,br=`
  fragment FileFragment on File {
    path
    isDir
    createdAt
    updatedAt
    size
    children
    mediaId
  }
`,xr=`
  fragment ImageFragment on Image {
    id
    title
    path
    size
    bucketId
    takenAt
    createdAt
    updatedAt
    tags {
      ...TagSubFragment
    }
  }
  ${$}
`,Sr=`
  fragment VideoFragment on Video {
    id
    title
    path
    duration
    size
    bucketId
    createdAt
    updatedAt
    takenAt
    tags {
      ...TagSubFragment
    }
  }
  ${$}
`,Cr=`
  fragment AudioFragment on Audio {
    id
    title
    artist
    path
    duration
    size
    bucketId
    albumFileId
    createdAt
    updatedAt
    tags {
      ...TagSubFragment
    }
  }
  ${$}
`,wr=`
  fragment NoteFragment on Note {
    id
    title
    content
    deletedAt
    createdAt
    updatedAt
    tags {
      ...TagSubFragment
    }
  }
  ${$}
`,Tr=`
  fragment DocFragment on Doc {
    id
    title
    path
    extension
    size
    bucketId
    createdAt
    updatedAt
    tags {
      ...TagSubFragment
    }
  }
  ${$}
`,Er=`
  fragment FeedFragment on Feed {
    id
    name
    url
    fetchContent
    createdAt
    updatedAt
  }
`,Dr=`
  fragment FeedEntryFragment on FeedEntry {
    id
    title
    url
    image
    author
    description
    content
    feedId
    rawId
    publishedAt
    createdAt
    updatedAt
    tags {
      ...TagSubFragment
    }
  }
  ${$}
`,Or=`
  fragment PackageFragment on Package {
    id
    name
    type
    version
    path
    size
    certs {
      issuer
      subject
      serialNumber
      validFrom
      validTo
    }
    installedAt
    updatedAt
  }
`,kr=`
  fragment NotificationFragment on Notification {
    id
    onlyOnce
    isClearable
    appId
    appName
    time
    silent
    title
    body
    actions
    replyActions
  }
`,Ar=`
  fragment DeviceInfoFragment on DeviceInfo {
    name
    platform
    manufacturer
    model
    osName
    osVersion
    kernelVersion
    appVersion
    appBuildNumber
    language
    uptime
    cpuArch
    totalMemory
    totalStorage
    display {
      width
      height
      density
    }
    android {
      sdkVersion
      versionCodeName
      securityPatch
      bootloader
      fingerprint
      hardware
      radioVersion
      board
      buildBrand
      buildHost
      buildUser
      buildNumber
      product
      device
      javaVmVersion
      glEsVersion
      serial
      buildTime
    }
    desktop {
      hostname
      cpuModel
      gpuModel
      desktopEnvironment
      windowManager
    }
  }
`,jr=`
  fragment BookmarkFragment on Bookmark {
    id
    url
    title
    faviconPath
    groupId
    pinned
    clickCount
    lastClickedAt
    sortOrder
    createdAt
    updatedAt
  }
`,Mr=`
  fragment BookmarkGroupFragment on BookmarkGroup {
    id
    name
    collapsed
    sortOrder
    createdAt
    updatedAt
  }
`,Nr=`
  fragment ChatChannelFragment on ChatChannel {
    id
    name
    owner
    members {
      ...ChatChannelMemberFragment
    }
    version
    status
    createdAt
    updatedAt
  }
  
  fragment ChatChannelMemberFragment on ChatChannelMember {
    id
    status
  }

`;function Pr(e){return e instanceof dr?e.status===403?`web_access_disabled`:e.message:`network_error`}function Fr(e){if(e)return typeof e==`function`?e():e}function Ir(e){let o=s(!1),c=s();async function l(t){o.value=!0;try{let n=t??Fr(e.variables),r=await lr(e.document,n);r.errors?.length?e.handle(r.data,r.errors[0].message):(c.value=r.data,e.handle(r.data,``))}catch(t){e.handle(void 0,Pr(t))}finally{o.value=!1}}if(l(),typeof e.variables==`function`){let o=!0;t()&&(a(()=>{o=!1}),n(()=>{o=!0})),i(e.variables,async()=>{await r(),o&&l()},{deep:!0})}return{loading:o,result:c,refetch:l}}function Lr(e){let t=s(!1),n=s();async function r(r){t.value=!0;try{let t=r??Fr(e.variables),i=await lr(e.document,t);i.errors?.length?e.handle(i.data,i.errors[0].message):(n.value=i.data,e.handle(i.data,``))}catch(t){e.handle(void 0,Pr(t))}finally{t.value=!1}}return{loading:t,result:n,fetch:r}}var Rr=`
  query ($id: String!) {
    chatItems(id: $id) {
      ...ChatItemFragment
    }
  }
  ${hr}
`,zr=`
  query {
    peers {
      id
      name
      ip
      status
      online
      port
      deviceType
      createdAt
      updatedAt
    }
  }
`,Br=`
  query {
    latestChatItems {
      ...ChatItemFragment
    }
  }
  ${hr}
`,Vr=`
  query appFiles($offset: Int!, $limit: Int!) {
    appFiles(offset: $offset, limit: $limit) {
      id
      size
      mimeType
      fileName
      createdAt
      updatedAt
    }
    appFileCount
  }
`,Hr=`
  query {
    chatChannels {
      ...ChatChannelFragment
    }
  }
  ${Nr}
`,Ur=`
  query ($id: ID!, $path: String!, $fileName: String!) {
    fileInfo(id: $id, path: $path, fileName: $fileName) {
      ... on FileInfo {
        path
        updatedAt
        size
        tags {
          ...TagSubFragment
        }
      }
      data {
        ... on ImageFileInfo {
          width
          height
          location {
            latitude
            longitude
          }
        }
        ... on VideoFileInfo {
          duration
          width
          height
          location {
            latitude
            longitude
          }
        }
        ... on AudioFileInfo {
          duration
          location {
            latitude
            longitude
          }
        }
      }
    }
  }
  ${$}
`,Wr=`
  query sms($offset: Int!, $limit: Int!, $query: String!) {
    sms(offset: $offset, limit: $limit, query: $query) {
      ...MessageFragment
    }
    smsCount(query: $query)
  }
  ${gr}
`,Gr=`
  query {
    sims {
      id
      label
      number
      subscriptionId
    }
  }
`,Kr=`
  query smsConversations($offset: Int!, $limit: Int!, $query: String!) {
    smsConversations(offset: $offset, limit: $limit, query: $query) {
      ...MessageConversationFragment
    }
    smsConversationCount(query: $query)
  }
  ${_r}
`,qr=`
  query contacts($offset: Int!, $limit: Int!, $query: String!) {
    contacts(offset: $offset, limit: $limit, query: $query) {
      ...ContactFragment
    }
    contactCount(query: $query)
  }
  ${vr}
`,Jr=`
  query homeStats($mediaQuery: String!) {
    smsCount(query: "")
    contactCount(query: "")
    callCount(query: "")
    imageCount(query: $mediaQuery)
    audioCount(query: $mediaQuery)
    videoCount(query: $mediaQuery)
    packageCount(query: "")
    noteCount(query: "")
    docCount(query: "")
    feedEntryCount(query: "")
    mounts {
      id
      path
      mountPoint
      totalBytes
      freeBytes
      driveType
    }
  }
`,Yr=`
  query {
    contactSources {
      name
      type
    }
  }
`,Xr=`
  query calls($offset: Int!, $limit: Int!, $query: String!) {
    calls(offset: $offset, limit: $limit, query: $query) {
      ...CallFragment
    }
    callCount(query: $query)
  }
  ${yr}
`,Zr=`
  query images($offset: Int!, $limit: Int!, $query: String!, $sortBy: FileSortBy!) {
    images(offset: $offset, limit: $limit, query: $query, sortBy: $sortBy) {
      ...ImageFragment
    }
    imageCount(query: $query)
  }
  ${xr}
`,Qr=`
  query {
    imageSearchStatus {
      status
      downloadProgress
      errorMessage
      modelSize
      modelDir
      isIndexing
      totalImages
      indexedImages
    }
  }
`,$r=`
  query videos($offset: Int!, $limit: Int!, $query: String!, $sortBy: FileSortBy!) {
    videos(offset: $offset, limit: $limit, query: $query, sortBy: $sortBy) {
      ...VideoFragment
    }
    videoCount(query: $query)
  }
  ${Sr}
`,ei=`
  query audios($offset: Int!, $limit: Int!, $query: String!, $sortBy: FileSortBy!) {
    items: audios(offset: $offset, limit: $limit, query: $query, sortBy: $sortBy) {
      ...AudioFragment
    }
    total: audioCount(query: $query)
  }
  ${Cr}
`,ti=`
  query files($root: String!, $offset: Int!, $limit: Int!, $query: String!, $sortBy: FileSortBy!) {
    files(root: $root, offset: $offset, limit: $limit, query: $query, sortBy: $sortBy) {
      ...FileFragment
    }
  }
  ${br}
`,ni=`
  query recentFiles {
    recentFiles {
      ...FileFragment
    }
  }
  ${br}
`,ri=`
  query {
    mounts {
      id
      name
      path
      mountPoint
      fsType
      totalBytes
      usedBytes
      freeBytes
      remote
      alias
      driveType
      diskID
    }
  }
`,ii=`
  query {
    app {
      ...AppFragment
    }
  }
  ${mr}
`,ai=`
  query tags($type: DataType!) {
    tags(type: $type) {
      ...TagFragment
    }
  }
  ${fr}
`,oi=`
  query mediaBuckets($type: DataType!) {
    mediaBuckets(type: $type) {
      id
      name
      itemCount
      topItems
    }
  }
`,si=`
  query notes($offset: Int!, $limit: Int!, $query: String!) {
    notes(offset: $offset, limit: $limit, query: $query) {
      id
      title
      deletedAt
      createdAt
      updatedAt
      tags {
        ...TagSubFragment
      }
    }
    noteCount(query: $query)
  }
  ${$}
`,ci=`
  query note($id: ID!) {
    note(id: $id) {
      ...NoteFragment
    }
  }
  ${wr}
`,li=`
  query {
    feeds {
      ...FeedFragment
    }
  }
  ${Er}
`,ui=`
  query feedEntries($offset: Int!, $limit: Int!, $query: String!) {
    items: feedEntries(offset: $offset, limit: $limit, query: $query) {
      id
      title
      url
      image
      author
      feedId
      rawId
      publishedAt
      createdAt
      updatedAt
      tags {
        ...TagSubFragment
      }
    }
    total: feedEntryCount(query: $query)
  }
  ${$}
`,di=`
  query feedsTags($type: DataType!) {
    tags(type: $type) {
      ...TagFragment
    }
    feeds {
      ...FeedFragment
    }
  }
  ${Er}
  ${fr}
`,fi=`
  query bucketsTags($type: DataType!) {
    tags(type: $type) {
      ...TagFragment
    }
    mediaBuckets(type: $type) {
      id
      name
      itemCount
      topItems
    }
  }
  ${fr}
`,pi=`
  query feedEntry($id: ID!) {
    feedEntry(id: $id) {
      ...FeedEntryFragment
      feed {
        ...FeedFragment
      }
    }
  }
  ${Er}
  ${Dr}
`,mi=`
  query imageCount($query: String!) {
    total: imageCount(query: $query)
    trash: imageCount(query: "trash:true")
  }
`,hi=`
  query audioCount($query: String!) {
    total: audioCount(query: $query)
    trash: audioCount(query: "trash:true")
  }
`,gi=`
  query docs($offset: Int!, $limit: Int!, $query: String!, $sortBy: FileSortBy!) {
    items: docs(offset: $offset, limit: $limit, query: $query, sortBy: $sortBy) {
      ...DocFragment
    }
    total: docCount(query: $query)
  }
  ${Tr}
`,_i=`
  query docCount($query: String!) {
    total: docCount(query: $query)
    trash: docCount(query: "trash:true")
    extGroups: docExtGroups {
      ext
      count
    }
  }
`,vi=`
  query videoCount($query: String!) {
    total: videoCount(query: $query)
    trash: videoCount(query: "trash:true")
  }
`,yi=`
  query {
    total: packageCount(query: "")
    system: packageCount(query: "type:system")
  }
`,bi=`
  query {
    total: feedEntryCount(query: "")
    today: feedEntryCount(query: "today:true")
    feedsCount {
      id
      count
    }
  }
`,xi=`
  query {
    total: contactCount(query: "")
  }
`,Si=`
  query {
    total: callCount(query: "")
    incoming: callCount(query: "type:1")
    outgoing: callCount(query: "type:2")
    missed: callCount(query: "type:3")
  }
`,Ci=`
  query {
    smsAllCounts {
      total
      inbox
      sent
      drafts
    }
  }
`,wi=`
  query {
    archivedConversations {
      ...MessageConversationFragment
    }
  }
  ${_r}
`,Ti=`
  query {
    total: noteCount(query: "")
    trash: noteCount(query: "trash:true")
  }
`,Ei=`
  query packages($offset: Int!, $limit: Int!, $query: String!, $sortBy: FileSortBy!) {
    packages(offset: $offset, limit: $limit, query: $query, sortBy: $sortBy) {
      ...PackageFragment
    }
    packageCount(query: $query)
  }
  ${Or}
`,Di=`
  query packageStatuses($ids: [ID!]!) {
    packageStatuses(ids: $ids) {
      id
      exist
      updatedAt
    }
  }
`,Oi=`
  query {
    screenMirrorState
    screenMirrorControlEnabled
    screenMirrorQuality {
      mode
      resolution
    }
  }
`,ki=`
  query {
    screenMirrorVideoCodec {
      annexB
      keyFrame
    }
  }
`,Ai=`
  query {
    screenMirrorControlEnabled
  }
`,ji=`
  query {
    notifications {
      ...NotificationFragment
    }
  }
  ${kr}
`,Mi=`
  query {
    deviceInfo {
      ...DeviceInfoFragment
    }
    sims {
      id
      label
      number
      subscriptionId
    }
    battery {
      level
      voltage
      health
      plugged
      temperature
      status
      technology
      capacity
    }
  }
  ${Ar}
`,Ni=`
  query AppLogs($offset: Int!, $limit: Int!) {
    appLogs(offset: $offset, limit: $limit)
  }
`,Pi=`
  query {
    appLogPath
  }
`,Fi=`
  query {
    dbPath
  }
`,Ii=`
  query {
    dataStorePath
  }
`,Li=`
  query uploadedChunks($fileId: String!) {
    uploadedChunks(fileId: $fileId)
  }
`,Ri=`
  query {
    pomodoroToday {
      date
      completedCount
      currentRound
      timeLeft
      totalTime
      isRunning
      isPause
      state
    }
    pomodoroSettings {
      workDuration
      shortBreakDuration
      longBreakDuration
      pomodorosBeforeLongBreak
      showNotification
      playSoundOnComplete
    }
  }
`,zi=`
  query {
    dataStoreEntries {
      key
      value
    }
  }
`,Bi=`
  query {
    dbTables
  }
`,Vi=`
  query DbTableRowCount($table: String!) {
    dbTableRowCount(table: $table)
  }
`,Hi=`
  query DbTableRows($table: String!, $offset: Int!, $limit: Int!) {
    dbTableRows(table: $table, offset: $offset, limit: $limit)
  }
`,Ui=`
  query DbTableInfo($table: String!) {
    dbTableInfo(table: $table) {
      idKey
    }
  }
`,Wi=`
  query {
    bookmarks {
      ...BookmarkFragment
    }
    bookmarkGroups {
      ...BookmarkGroupFragment
    }
  }
  ${jr}
  ${Mr}
`,Gi=`
  query {
    isDiscovering
  }
`,Ki=`plain-web:store:`,qi=new Map;function Ji(e){let t=qi.get(e);if(t)return t;let n=new BroadcastChannel(Ki+e),r=new Set;return n.onmessage=e=>{let t=e.data;if(!(!t||t.windowId===I())&&t.clientId===N())for(let e of r)e(t.patch)},t={bc:n,subscribers:r},qi.set(e,t),t}var Yi=globalThis.__plainWebInstalled??new WeakSet;globalThis.__plainWebInstalled=Yi;function Xi(e,t,n){if(Yi.has(e))return;Yi.add(e);let r=Ji(t);r.subscribers.add(t=>{e.__cw_replaying=!0;try{e.$patch(t)}finally{queueMicrotask(()=>{e.__cw_replaying=!1})}}),e.$subscribe((t,i)=>{if(e.__cw_replaying)return;let a={};for(let e of n)a[e]=c(i[e]);let o=JSON.parse(JSON.stringify(a)),s={windowId:I(),clientId:N(),patch:o};r.bc.postMessage(s)},{detached:!0})}function Zi(e,t,n){let r=tr(e,t),{syncKeys:i}=n;return(()=>{let t=r();return Xi(t,e,i),t})}var Qi=Zi(`temp`,{state:()=>({app:{clientId:``},urlTokenKey:null,uploads:[],selectedFiles:[],audioPlaying:!1,lightbox:{sources:[],visible:!1,index:-1},counter:{messages:-1,contacts:-1,calls:-1,videos:-1,videosTrash:-1,images:-1,imagesTrash:-1,audios:-1,audiosTrash:-1,packages:-1,packagesSystem:-1,notes:-1,notesTrash:-1,docs:-1,docsTrash:-1,docExtGroups:[],feedEntries:-1,feedEntriesToday:-1,total:-1,free:-1},feedsSyncing:!1})},{syncKeys:[`counter`,`audioPlaying`,`feedsSyncing`]});export{Ri as $,zt as $t,pi as A,Sn as At,Ir as B,hn as Bt,Hi as C,dr as Ct,gi as D,nr as Dt,_i as E,tr as Et,Jr as F,_n as Ft,Ti as G,Jt as Gt,Br as H,pn as Ht,mi as I,fn as It,ji as J,Zt as Jt,ci as K,Xt as Kt,Qr as L,sn as Lt,di as M,dn as Mt,Ur as N,mn as Nt,ui as O,xn as Ot,ti as P,vn as Pt,zr as Q,Vt as Qt,Zr as R,cn as Rt,Vi as S,fr as St,Mi as T,Hn as Tt,oi as U,nn as Ut,Gi as V,on as Vt,ri as W,qt as Wt,Di as X,Wt as Xt,yi as Y,an as Yt,Ei as Z,Gt as Zt,qr as _,P as _n,Dr as _t,Ni as a,le as an,Kr as at,Fi as b,wr as bt,ei as c,se as cn,ai as ct,Si as d,B as dn,$r as dt,Rt as en,ni as et,Xr as f,H as fn,jr as ft,Yr as g,N as gn,vr as gt,xi as h,A as hn,hr as ht,Pi as i,de as in,Gr as it,li as j,un as jt,bi as k,Cn as kt,Wi as l,ue as ln,Li as lt,Rr as m,M as mn,Nr as mt,Vr as n,It as nn,Oi as nt,wi as o,ae as on,Ci as ot,Hr as p,L as pn,Mr as pt,si as q,Qt as qt,ii as r,ne as rn,ki as rt,hi as s,re as sn,Wr as st,Qi as t,Lt as tn,Ai as tt,fi as u,te as un,vi as ut,zi as v,j as vn,Er as vt,Bi as w,lr as wt,Ui as x,pr as xt,Ii as y,S as yn,br as yt,Lr as z,gn as zt};