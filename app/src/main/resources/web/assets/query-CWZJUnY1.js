import{E as e,F as t,P as n,Q as r,R as i,ct as a}from"./vue.runtime.esm-bundler-DB7W0Wog.js";import{i as o,n as s}from"./prefs-BsL_qHAr.js";var c=`__bound_client_id__`,l=`__window_id__`,u,d;function f(e){try{return sessionStorage.getItem(e)}catch{return null}}function p(e,t){try{t?sessionStorage.setItem(e,t):sessionStorage.removeItem(e)}catch{}}function m(){return s(`client_id`,``)}function h(){if(u!==void 0)return u??``;let e=f(c)??``;return u=e,e}function g(e){u=e,p(c,e)}function _(){u=``,p(c,``)}function v(){return h()||m()}function y(){return!h()}function b(e){if(!e)return!0;let t=m();return!t||e===t}function x(){if(d)return d;let e=f(l);return e||(e=`w_${Math.random().toString(36).slice(2,10)}_${Date.now().toString(36)}`,p(l,e)),d=e,e}function S(){try{let e=new URL(window.location.href),t=e.searchParams.get(`__cid`);if(!t)return;if(b(t)){e.searchParams.delete(`__cid`);let t=e.searchParams.toString(),n=e.pathname+(t?`?${t}`:``)+e.hash;window.history.replaceState({},``,n);return}g(t),e.searchParams.delete(`__cid`);let n=e.searchParams.toString(),r=e.pathname+(n?`?${n}`:``)+e.hash;window.history.replaceState({},``,r)}catch{}}var C=`device_sessions`;function w(){try{let e=s(C,null);return!e||!Array.isArray(e.sessions)?{sessions:[]}:{sessions:e.sessions}}catch{return{sessions:[]}}}function T(){return v()}function E(){let{sessions:e}=w();return e.find(e=>e.clientId===T())?.token??``}function D(){try{let e=s(C,null);if(!e)return;let t=T();if(t&&Array.isArray(e.sessions)){let n=e.sessions.find(e=>e.clientId===t);n&&(n.token=``),o(C,e)}_()}catch{}}function O(){let e=T();return e?`main_state:${e}`:`main_state`}var k=``;function A(e){k=e}function j(){k=``}function M(){return k}var N=``;function P(){return N}function F(e){return`${B()}${e}`}function I(){return B()}function L(){return window.location.host}function R(){return{"Content-Type":`multipart/form-data`,"c-id":s(`client_id`,``)}}function z(){return`${window.location.protocol===`http:`?`ws`:`wss`}://${L()}`}function B(){return`${window.location.protocol}//${L()}`}function V(){try{return new URL(`http://${L()}`).hostname}catch{return L().split(`:`)[0]}}function H(e){return e instanceof Uint8Array||ArrayBuffer.isView(e)&&e.constructor.name===`Uint8Array`&&`BYTES_PER_ELEMENT`in e&&e.BYTES_PER_ELEMENT===1}function U(e,t,n=``){let r=H(e),i=e?.length,a=t!==void 0;if(!r||a&&i!==t){let o=n&&`"${n}" `,s=a?` of length ${t}`:``,c=r?`length=${i}`:`type=${typeof e}`,l=o+`expected Uint8Array`+s+`, got `+c;throw r?RangeError(l):TypeError(l)}return e}function W(e,t=!0){if(e.destroyed)throw Error(`Hash instance has been destroyed`);if(t&&e.finished)throw Error(`Hash#digest() has already been called`)}function ee(e,t){U(e,void 0,`digestInto() output`);let n=t.outputLen;if(e.length<n)throw RangeError(`"digestInto() output" expected to be of length >=`+n)}function te(...e){for(let t=0;t<e.length;t++)e[t].fill(0)}function ne(e){return new DataView(e.buffer,e.byteOffset,e.byteLength)}var re=typeof Uint8Array.from([]).toHex==`function`&&typeof Uint8Array.fromHex==`function`,ie=Array.from({length:256},(e,t)=>t.toString(16).padStart(2,`0`));function ae(e){if(U(e),re)return e.toHex();let t=``;for(let n=0;n<e.length;n++)t+=ie[e[n]];return t}function oe(e,t={}){let n=(t,n)=>e(n).update(t).digest(),r=e(void 0);return n.outputLen=r.outputLen,n.blockLen=r.blockLen,n.canXOF=r.canXOF,n.create=t=>e(t),Object.assign(n,t),Object.freeze(n)}var se=e=>({oid:Uint8Array.from([6,9,96,134,72,1,101,3,4,2,e])}),ce=class{blockLen;outputLen;canXOF=!1;padOffset;isLE;buffer;view;finished=!1;length=0;pos=0;destroyed=!1;constructor(e,t,n,r){this.blockLen=e,this.outputLen=t,this.padOffset=n,this.isLE=r,this.buffer=new Uint8Array(e),this.view=ne(this.buffer)}update(e){W(this),U(e);let{view:t,buffer:n,blockLen:r}=this,i=e.length;for(let a=0;a<i;){let o=Math.min(r-this.pos,i-a);if(o===r){let t=ne(e);for(;r<=i-a;a+=r)this.process(t,a);continue}n.set(e.subarray(a,a+o),this.pos),this.pos+=o,a+=o,this.pos===r&&(this.process(t,0),this.pos=0)}return this.length+=e.length,this.roundClean(),this}digestInto(e){W(this),ee(e,this),this.finished=!0;let{buffer:t,view:n,blockLen:r,isLE:i}=this,{pos:a}=this;t[a++]=128,te(this.buffer.subarray(a)),this.padOffset>r-a&&(this.process(n,0),a=0);for(let e=a;e<r;e++)t[e]=0;n.setBigUint64(r-8,BigInt(this.length*8),i),this.process(n,0);let o=ne(e),s=this.outputLen;if(s%4)throw Error(`_sha2: outputLen must be aligned to 32bit`);let c=s/4,l=this.get();if(c>l.length)throw Error(`_sha2: outputLen bigger than state`);for(let e=0;e<c;e++)o.setUint32(4*e,l[e],i)}digest(){let{buffer:e,outputLen:t}=this;this.digestInto(e);let n=e.slice(0,t);return this.destroy(),n}_cloneInto(e){e||=new this.constructor,e.set(...this.get());let{blockLen:t,buffer:n,length:r,finished:i,destroyed:a,pos:o}=this;return e.destroyed=a,e.finished=i,e.length=r,e.pos=o,r%t&&e.buffer.set(n),e}clone(){return this._cloneInto()}},G=Uint32Array.from([1779033703,4089235720,3144134277,2227873595,1013904242,4271175723,2773480762,1595750129,1359893119,2917565137,2600822924,725511199,528734635,4215389547,1541459225,327033209]),le=BigInt(2**32-1),ue=BigInt(32);function de(e,t=!1){return t?{h:Number(e&le),l:Number(e>>ue&le)}:{h:Number(e>>ue&le)|0,l:Number(e&le)|0}}function fe(e,t=!1){let n=e.length,r=new Uint32Array(n),i=new Uint32Array(n);for(let a=0;a<n;a++){let{h:n,l:o}=de(e[a],t);[r[a],i[a]]=[n,o]}return[r,i]}var pe=(e,t,n)=>e>>>n,me=(e,t,n)=>e<<32-n|t>>>n,he=(e,t,n)=>e>>>n|t<<32-n,ge=(e,t,n)=>e<<32-n|t>>>n,_e=(e,t,n)=>e<<64-n|t>>>n-32,ve=(e,t,n)=>e>>>n-32|t<<64-n;function K(e,t,n,r){let i=(t>>>0)+(r>>>0);return{h:e+n+(i/2**32|0)|0,l:i|0}}var ye=(e,t,n)=>(e>>>0)+(t>>>0)+(n>>>0),be=(e,t,n,r)=>t+n+r+(e/2**32|0)|0,xe=(e,t,n,r)=>(e>>>0)+(t>>>0)+(n>>>0)+(r>>>0),Se=(e,t,n,r,i)=>t+n+r+i+(e/2**32|0)|0,Ce=(e,t,n,r,i)=>(e>>>0)+(t>>>0)+(n>>>0)+(r>>>0)+(i>>>0),we=(e,t,n,r,i,a)=>t+n+r+i+a+(e/2**32|0)|0,Te=fe(`0x428a2f98d728ae22.0x7137449123ef65cd.0xb5c0fbcfec4d3b2f.0xe9b5dba58189dbbc.0x3956c25bf348b538.0x59f111f1b605d019.0x923f82a4af194f9b.0xab1c5ed5da6d8118.0xd807aa98a3030242.0x12835b0145706fbe.0x243185be4ee4b28c.0x550c7dc3d5ffb4e2.0x72be5d74f27b896f.0x80deb1fe3b1696b1.0x9bdc06a725c71235.0xc19bf174cf692694.0xe49b69c19ef14ad2.0xefbe4786384f25e3.0x0fc19dc68b8cd5b5.0x240ca1cc77ac9c65.0x2de92c6f592b0275.0x4a7484aa6ea6e483.0x5cb0a9dcbd41fbd4.0x76f988da831153b5.0x983e5152ee66dfab.0xa831c66d2db43210.0xb00327c898fb213f.0xbf597fc7beef0ee4.0xc6e00bf33da88fc2.0xd5a79147930aa725.0x06ca6351e003826f.0x142929670a0e6e70.0x27b70a8546d22ffc.0x2e1b21385c26c926.0x4d2c6dfc5ac42aed.0x53380d139d95b3df.0x650a73548baf63de.0x766a0abb3c77b2a8.0x81c2c92e47edaee6.0x92722c851482353b.0xa2bfe8a14cf10364.0xa81a664bbc423001.0xc24b8b70d0f89791.0xc76c51a30654be30.0xd192e819d6ef5218.0xd69906245565a910.0xf40e35855771202a.0x106aa07032bbd1b8.0x19a4c116b8d2d0c8.0x1e376c085141ab53.0x2748774cdf8eeb99.0x34b0bcb5e19b48a8.0x391c0cb3c5c95a63.0x4ed8aa4ae3418acb.0x5b9cca4f7763e373.0x682e6ff3d6b2b8a3.0x748f82ee5defb2fc.0x78a5636f43172f60.0x84c87814a1f0ab72.0x8cc702081a6439ec.0x90befffa23631e28.0xa4506cebde82bde9.0xbef9a3f7b2c67915.0xc67178f2e372532b.0xca273eceea26619c.0xd186b8c721c0c207.0xeada7dd6cde0eb1e.0xf57d4f7fee6ed178.0x06f067aa72176fba.0x0a637dc5a2c898a6.0x113f9804bef90dae.0x1b710b35131c471b.0x28db77f523047d84.0x32caab7b40c72493.0x3c9ebe0a15c9bebc.0x431d67c49c100d4c.0x4cc5d4becb3e42b6.0x597f299cfc657e2a.0x5fcb6fab3ad6faec.0x6c44198c4a475817`.split(`.`).map(e=>BigInt(e))),Ee=Te[0],De=Te[1],q=new Uint32Array(80),J=new Uint32Array(80),Oe=class extends ce{constructor(e){super(128,e,16,!1)}get(){let{Ah:e,Al:t,Bh:n,Bl:r,Ch:i,Cl:a,Dh:o,Dl:s,Eh:c,El:l,Fh:u,Fl:d,Gh:f,Gl:p,Hh:m,Hl:h}=this;return[e,t,n,r,i,a,o,s,c,l,u,d,f,p,m,h]}set(e,t,n,r,i,a,o,s,c,l,u,d,f,p,m,h){this.Ah=e|0,this.Al=t|0,this.Bh=n|0,this.Bl=r|0,this.Ch=i|0,this.Cl=a|0,this.Dh=o|0,this.Dl=s|0,this.Eh=c|0,this.El=l|0,this.Fh=u|0,this.Fl=d|0,this.Gh=f|0,this.Gl=p|0,this.Hh=m|0,this.Hl=h|0}process(e,t){for(let n=0;n<16;n++,t+=4)q[n]=e.getUint32(t),J[n]=e.getUint32(t+=4);for(let e=16;e<80;e++){let t=q[e-15]|0,n=J[e-15]|0,r=he(t,n,1)^he(t,n,8)^pe(t,n,7),i=ge(t,n,1)^ge(t,n,8)^me(t,n,7),a=q[e-2]|0,o=J[e-2]|0,s=he(a,o,19)^_e(a,o,61)^pe(a,o,6),c=xe(i,ge(a,o,19)^ve(a,o,61)^me(a,o,6),J[e-7],J[e-16]);q[e]=Se(c,r,s,q[e-7],q[e-16])|0,J[e]=c|0}let{Ah:n,Al:r,Bh:i,Bl:a,Ch:o,Cl:s,Dh:c,Dl:l,Eh:u,El:d,Fh:f,Fl:p,Gh:m,Gl:h,Hh:g,Hl:_}=this;for(let e=0;e<80;e++){let t=he(u,d,14)^he(u,d,18)^_e(u,d,41),v=ge(u,d,14)^ge(u,d,18)^ve(u,d,41),y=u&f^~u&m,b=d&p^~d&h,x=Ce(_,v,b,De[e],J[e]),S=we(x,g,t,y,Ee[e],q[e]),C=x|0,w=he(n,r,28)^_e(n,r,34)^_e(n,r,39),T=ge(n,r,28)^ve(n,r,34)^ve(n,r,39),E=n&i^n&o^i&o,D=r&a^r&s^a&s;g=m|0,_=h|0,m=f|0,h=p|0,f=u|0,p=d|0,{h:u,l:d}=K(c|0,l|0,S|0,C|0),c=o|0,l=s|0,o=i|0,s=a|0,i=n|0,a=r|0;let O=ye(C,T,D);n=be(O,S,w,E),r=O|0}({h:n,l:r}=K(this.Ah|0,this.Al|0,n|0,r|0)),{h:i,l:a}=K(this.Bh|0,this.Bl|0,i|0,a|0),{h:o,l:s}=K(this.Ch|0,this.Cl|0,o|0,s|0),{h:c,l}=K(this.Dh|0,this.Dl|0,c|0,l|0),{h:u,l:d}=K(this.Eh|0,this.El|0,u|0,d|0),{h:f,l:p}=K(this.Fh|0,this.Fl|0,f|0,p|0),{h:m,l:h}=K(this.Gh|0,this.Gl|0,m|0,h|0),{h:g,l:_}=K(this.Hh|0,this.Hl|0,g|0,_|0),this.set(n,r,i,a,o,s,c,l,u,d,f,p,m,h,g,_)}roundClean(){te(q,J)}destroy(){this.destroyed=!0,te(this.buffer),this.set(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0)}},ke=class extends Oe{Ah=G[0]|0;Al=G[1]|0;Bh=G[2]|0;Bl=G[3]|0;Ch=G[4]|0;Cl=G[5]|0;Dh=G[6]|0;Dl=G[7]|0;Eh=G[8]|0;El=G[9]|0;Fh=G[10]|0;Fl=G[11]|0;Gh=G[12]|0;Gl=G[13]|0;Hh=G[14]|0;Hl=G[15]|0;constructor(){super(64)}},Ae=oe(()=>new ke,se(3));function je(e){return ae(Ae(new TextEncoder().encode(e)))}function Me(e){return e instanceof Uint8Array||ArrayBuffer.isView(e)&&e.constructor.name===`Uint8Array`}function Ne(e){if(typeof e!=`boolean`)throw Error(`boolean expected, not ${e}`)}function Pe(e){if(!Number.isSafeInteger(e)||e<0)throw Error(`positive integer expected, got `+e)}function Y(e,...t){if(!Me(e))throw Error(`Uint8Array expected`);if(t.length>0&&!t.includes(e.length))throw Error(`Uint8Array expected of length `+t+`, got length=`+e.length)}function Fe(e,t=!0){if(e.destroyed)throw Error(`Hash instance has been destroyed`);if(t&&e.finished)throw Error(`Hash#digest() has already been called`)}function Ie(e,t){Y(e);let n=t.outputLen;if(e.length<n)throw Error(`digestInto() expects output buffer of length at least `+n)}function X(e){return new Uint32Array(e.buffer,e.byteOffset,Math.floor(e.byteLength/4))}function Le(...e){for(let t=0;t<e.length;t++)e[t].fill(0)}function Re(e){return new DataView(e.buffer,e.byteOffset,e.byteLength)}var ze=new Uint8Array(new Uint32Array([287454020]).buffer)[0]===68;function Be(e){if(typeof e!=`string`)throw Error(`string expected`);return new Uint8Array(new TextEncoder().encode(e))}function Ve(e){if(typeof e==`string`)e=Be(e);else if(Me(e))e=Ye(e);else throw Error(`Uint8Array expected, got `+typeof e);return e}function He(e,t){if(typeof t!=`object`||!t)throw Error(`options must be defined`);return Object.assign(e,t)}function Ue(e,t){if(e.length!==t.length)return!1;let n=0;for(let r=0;r<e.length;r++)n|=e[r]^t[r];return n===0}var We=(e,t)=>{function n(n,...r){if(Y(n),!ze)throw Error(`Non little-endian hardware is not yet supported`);if(e.nonceLength!==void 0){let t=r[0];if(!t)throw Error(`nonce / iv required`);e.varSizeNonce?Y(t):Y(t,e.nonceLength)}let i=e.tagLength;i&&r[1]!==void 0&&Y(r[1]);let a=t(n,...r),o=(e,t)=>{if(t!==void 0){if(e!==2)throw Error(`cipher output not supported`);Y(t)}},s=!1;return{encrypt(e,t){if(s)throw Error(`cannot encrypt() twice with same key + nonce`);return s=!0,Y(e),o(a.encrypt.length,t),a.encrypt(e,t)},decrypt(e,t){if(Y(e),i&&e.length<i)throw Error(`invalid ciphertext length: smaller than tagLength=`+i);return o(a.decrypt.length,t),a.decrypt(e,t)}}}return Object.assign(n,e),n};function Ge(e,t,n=!0){if(t===void 0)return new Uint8Array(e);if(t.length!==e)throw Error(`invalid output length, expected `+e+`, got: `+t.length);if(n&&!Je(t))throw Error(`invalid output, must be aligned`);return t}function Ke(e,t,n,r){if(typeof e.setBigUint64==`function`)return e.setBigUint64(t,n,r);let i=BigInt(32),a=BigInt(4294967295),o=Number(n>>i&a),s=Number(n&a),c=r?4:0,l=r?0:4;e.setUint32(t+c,o,r),e.setUint32(t+l,s,r)}function qe(e,t,n){Ne(n);let r=new Uint8Array(16),i=Re(r);return Ke(i,0,BigInt(t),n),Ke(i,8,BigInt(e),n),r}function Je(e){return e.byteOffset%4==0}function Ye(e){return Uint8Array.from(e)}var Xe=e=>Uint8Array.from(e.split(``).map(e=>e.charCodeAt(0))),Ze=Xe(`expand 16-byte k`),Qe=Xe(`expand 32-byte k`),$e=X(Ze),et=X(Qe);function Z(e,t){return e<<t|e>>>32-t}function tt(e){return e.byteOffset%4==0}var nt=64,rt=16,it=2**32-1,at=new Uint32Array;function ot(e,t,n,r,i,a,o,s){let c=i.length,l=new Uint8Array(nt),u=X(l),d=tt(i)&&tt(a),f=d?X(i):at,p=d?X(a):at;for(let m=0;m<c;o++){if(e(t,n,r,u,o,s),o>=it)throw Error(`arx: counter overflow`);let h=Math.min(nt,c-m);if(d&&h===nt){let e=m/4;if(m%4!=0)throw Error(`arx: invalid block position`);for(let t=0,n;t<rt;t++)n=e+t,p[n]=f[n]^u[t];m+=nt;continue}for(let e=0,t;e<h;e++)t=m+e,a[t]=i[t]^l[e];m+=h}}function st(e,t){let{allowShortKeys:n,extendNonceFn:r,counterLength:i,counterRight:a,rounds:o}=He({allowShortKeys:!1,counterLength:8,counterRight:!1,rounds:20},t);if(typeof e!=`function`)throw Error(`core must be a function`);return Pe(i),Pe(o),Ne(a),Ne(n),(t,s,c,l,u=0)=>{Y(t),Y(s),Y(c);let d=c.length;if(l===void 0&&(l=new Uint8Array(d)),Y(l),Pe(u),u<0||u>=it)throw Error(`arx: counter overflow`);if(l.length<d)throw Error(`arx: output (${l.length}) is shorter than data (${d})`);let f=[],p=t.length,m,h;if(p===32)f.push(m=Ye(t)),h=et;else if(p===16&&n)m=new Uint8Array(32),m.set(t),m.set(t,16),h=$e,f.push(m);else throw Error(`arx: invalid 32-byte key, got length=${p}`);tt(s)||f.push(s=Ye(s));let g=X(m);if(r){if(s.length!==24)throw Error(`arx: extended nonce must be 24 bytes`);r(h,g,X(s.subarray(0,16)),g),s=s.subarray(16)}let _=16-i;if(_!==s.length)throw Error(`arx: nonce must be ${_} or 16 bytes`);if(_!==12){let e=new Uint8Array(12);e.set(s,a?0:12-s.length),s=e,f.push(s)}let v=X(s);return ot(e,h,g,v,c,l,u,o),Le(...f),l}}var Q=(e,t)=>e[t++]&255|(e[t++]&255)<<8,ct=class{constructor(e){this.blockLen=16,this.outputLen=16,this.buffer=new Uint8Array(16),this.r=new Uint16Array(10),this.h=new Uint16Array(10),this.pad=new Uint16Array(8),this.pos=0,this.finished=!1,e=Ve(e),Y(e,32);let t=Q(e,0),n=Q(e,2),r=Q(e,4),i=Q(e,6),a=Q(e,8),o=Q(e,10),s=Q(e,12),c=Q(e,14);this.r[0]=t&8191,this.r[1]=(t>>>13|n<<3)&8191,this.r[2]=(n>>>10|r<<6)&7939,this.r[3]=(r>>>7|i<<9)&8191,this.r[4]=(i>>>4|a<<12)&255,this.r[5]=a>>>1&8190,this.r[6]=(a>>>14|o<<2)&8191,this.r[7]=(o>>>11|s<<5)&8065,this.r[8]=(s>>>8|c<<8)&8191,this.r[9]=c>>>5&127;for(let t=0;t<8;t++)this.pad[t]=Q(e,16+2*t)}process(e,t,n=!1){let r=n?0:2048,{h:i,r:a}=this,o=a[0],s=a[1],c=a[2],l=a[3],u=a[4],d=a[5],f=a[6],p=a[7],m=a[8],h=a[9],g=Q(e,t+0),_=Q(e,t+2),v=Q(e,t+4),y=Q(e,t+6),b=Q(e,t+8),x=Q(e,t+10),S=Q(e,t+12),C=Q(e,t+14),w=i[0]+(g&8191),T=i[1]+((g>>>13|_<<3)&8191),E=i[2]+((_>>>10|v<<6)&8191),D=i[3]+((v>>>7|y<<9)&8191),O=i[4]+((y>>>4|b<<12)&8191),k=i[5]+(b>>>1&8191),A=i[6]+((b>>>14|x<<2)&8191),j=i[7]+((x>>>11|S<<5)&8191),M=i[8]+((S>>>8|C<<8)&8191),N=i[9]+(C>>>5|r),P=0,F=P+w*o+5*h*T+5*m*E+5*p*D+5*f*O;P=F>>>13,F&=8191,F+=5*d*k+5*u*A+5*l*j+5*c*M+5*s*N,P+=F>>>13,F&=8191;let I=P+w*s+T*o+5*h*E+5*m*D+5*p*O;P=I>>>13,I&=8191,I+=5*f*k+5*d*A+5*u*j+5*l*M+5*c*N,P+=I>>>13,I&=8191;let L=P+w*c+T*s+E*o+5*h*D+5*m*O;P=L>>>13,L&=8191,L+=5*p*k+5*f*A+5*d*j+5*u*M+5*l*N,P+=L>>>13,L&=8191;let R=P+w*l+T*c+E*s+D*o+5*h*O;P=R>>>13,R&=8191,R+=5*m*k+5*p*A+5*f*j+5*d*M+5*u*N,P+=R>>>13,R&=8191;let z=P+w*u+T*l+E*c+D*s+O*o;P=z>>>13,z&=8191,z+=5*h*k+5*m*A+5*p*j+5*f*M+5*d*N,P+=z>>>13,z&=8191;let B=P+w*d+T*u+E*l+D*c+O*s;P=B>>>13,B&=8191,B+=k*o+5*h*A+5*m*j+5*p*M+5*f*N,P+=B>>>13,B&=8191;let V=P+w*f+T*d+E*u+D*l+O*c;P=V>>>13,V&=8191,V+=k*s+A*o+5*h*j+5*m*M+5*p*N,P+=V>>>13,V&=8191;let H=P+w*p+T*f+E*d+D*u+O*l;P=H>>>13,H&=8191,H+=k*c+A*s+j*o+5*h*M+5*m*N,P+=H>>>13,H&=8191;let U=P+w*m+T*p+E*f+D*d+O*u;P=U>>>13,U&=8191,U+=k*l+A*c+j*s+M*o+5*h*N,P+=U>>>13,U&=8191;let W=P+w*h+T*m+E*p+D*f+O*d;P=W>>>13,W&=8191,W+=k*u+A*l+j*c+M*s+N*o,P+=W>>>13,W&=8191,P=(P<<2)+P|0,P=P+F|0,F=P&8191,P>>>=13,I+=P,i[0]=F,i[1]=I,i[2]=L,i[3]=R,i[4]=z,i[5]=B,i[6]=V,i[7]=H,i[8]=U,i[9]=W}finalize(){let{h:e,pad:t}=this,n=new Uint16Array(10),r=e[1]>>>13;e[1]&=8191;for(let t=2;t<10;t++)e[t]+=r,r=e[t]>>>13,e[t]&=8191;e[0]+=r*5,r=e[0]>>>13,e[0]&=8191,e[1]+=r,r=e[1]>>>13,e[1]&=8191,e[2]+=r,n[0]=e[0]+5,r=n[0]>>>13,n[0]&=8191;for(let t=1;t<10;t++)n[t]=e[t]+r,r=n[t]>>>13,n[t]&=8191;n[9]-=8192;let i=(r^1)-1;for(let e=0;e<10;e++)n[e]&=i;i=~i;for(let t=0;t<10;t++)e[t]=e[t]&i|n[t];e[0]=(e[0]|e[1]<<13)&65535,e[1]=(e[1]>>>3|e[2]<<10)&65535,e[2]=(e[2]>>>6|e[3]<<7)&65535,e[3]=(e[3]>>>9|e[4]<<4)&65535,e[4]=(e[4]>>>12|e[5]<<1|e[6]<<14)&65535,e[5]=(e[6]>>>2|e[7]<<11)&65535,e[6]=(e[7]>>>5|e[8]<<8)&65535,e[7]=(e[8]>>>8|e[9]<<5)&65535;let a=e[0]+t[0];e[0]=a&65535;for(let n=1;n<8;n++)a=(e[n]+t[n]|0)+(a>>>16)|0,e[n]=a&65535;Le(n)}update(e){Fe(this),e=Ve(e),Y(e);let{buffer:t,blockLen:n}=this,r=e.length;for(let i=0;i<r;){let a=Math.min(n-this.pos,r-i);if(a===n){for(;n<=r-i;i+=n)this.process(e,i);continue}t.set(e.subarray(i,i+a),this.pos),this.pos+=a,i+=a,this.pos===n&&(this.process(t,0,!1),this.pos=0)}return this}destroy(){Le(this.h,this.r,this.buffer,this.pad)}digestInto(e){Fe(this),Ie(e,this),this.finished=!0;let{buffer:t,h:n}=this,{pos:r}=this;if(r){for(t[r++]=1;r<16;r++)t[r]=0;this.process(t,0,!0)}this.finalize();let i=0;for(let t=0;t<8;t++)e[i++]=n[t]>>>0,e[i++]=n[t]>>>8;return e}digest(){let{buffer:e,outputLen:t}=this;this.digestInto(e);let n=e.slice(0,t);return this.destroy(),n}};function lt(e){let t=(t,n)=>e(n).update(Ve(t)).digest(),n=e(new Uint8Array(32));return t.outputLen=n.outputLen,t.blockLen=n.blockLen,t.create=t=>e(t),t}var ut=lt(e=>new ct(e));function dt(e,t,n,r,i,a=20){let o=e[0],s=e[1],c=e[2],l=e[3],u=t[0],d=t[1],f=t[2],p=t[3],m=t[4],h=t[5],g=t[6],_=t[7],v=i,y=n[0],b=n[1],x=n[2],S=o,C=s,w=c,T=l,E=u,D=d,O=f,k=p,A=m,j=h,M=g,N=_,P=v,F=y,I=b,L=x;for(let e=0;e<a;e+=2)S=S+E|0,P=Z(P^S,16),A=A+P|0,E=Z(E^A,12),S=S+E|0,P=Z(P^S,8),A=A+P|0,E=Z(E^A,7),C=C+D|0,F=Z(F^C,16),j=j+F|0,D=Z(D^j,12),C=C+D|0,F=Z(F^C,8),j=j+F|0,D=Z(D^j,7),w=w+O|0,I=Z(I^w,16),M=M+I|0,O=Z(O^M,12),w=w+O|0,I=Z(I^w,8),M=M+I|0,O=Z(O^M,7),T=T+k|0,L=Z(L^T,16),N=N+L|0,k=Z(k^N,12),T=T+k|0,L=Z(L^T,8),N=N+L|0,k=Z(k^N,7),S=S+D|0,L=Z(L^S,16),M=M+L|0,D=Z(D^M,12),S=S+D|0,L=Z(L^S,8),M=M+L|0,D=Z(D^M,7),C=C+O|0,P=Z(P^C,16),N=N+P|0,O=Z(O^N,12),C=C+O|0,P=Z(P^C,8),N=N+P|0,O=Z(O^N,7),w=w+k|0,F=Z(F^w,16),A=A+F|0,k=Z(k^A,12),w=w+k|0,F=Z(F^w,8),A=A+F|0,k=Z(k^A,7),T=T+E|0,I=Z(I^T,16),j=j+I|0,E=Z(E^j,12),T=T+E|0,I=Z(I^T,8),j=j+I|0,E=Z(E^j,7);let R=0;r[R++]=o+S|0,r[R++]=s+C|0,r[R++]=c+w|0,r[R++]=l+T|0,r[R++]=u+E|0,r[R++]=d+D|0,r[R++]=f+O|0,r[R++]=p+k|0,r[R++]=m+A|0,r[R++]=h+j|0,r[R++]=g+M|0,r[R++]=_+N|0,r[R++]=v+P|0,r[R++]=y+F|0,r[R++]=b+I|0,r[R++]=x+L|0}function ft(e,t,n,r){let i=e[0],a=e[1],o=e[2],s=e[3],c=t[0],l=t[1],u=t[2],d=t[3],f=t[4],p=t[5],m=t[6],h=t[7],g=n[0],_=n[1],v=n[2],y=n[3];for(let e=0;e<20;e+=2)i=i+c|0,g=Z(g^i,16),f=f+g|0,c=Z(c^f,12),i=i+c|0,g=Z(g^i,8),f=f+g|0,c=Z(c^f,7),a=a+l|0,_=Z(_^a,16),p=p+_|0,l=Z(l^p,12),a=a+l|0,_=Z(_^a,8),p=p+_|0,l=Z(l^p,7),o=o+u|0,v=Z(v^o,16),m=m+v|0,u=Z(u^m,12),o=o+u|0,v=Z(v^o,8),m=m+v|0,u=Z(u^m,7),s=s+d|0,y=Z(y^s,16),h=h+y|0,d=Z(d^h,12),s=s+d|0,y=Z(y^s,8),h=h+y|0,d=Z(d^h,7),i=i+l|0,y=Z(y^i,16),m=m+y|0,l=Z(l^m,12),i=i+l|0,y=Z(y^i,8),m=m+y|0,l=Z(l^m,7),a=a+u|0,g=Z(g^a,16),h=h+g|0,u=Z(u^h,12),a=a+u|0,g=Z(g^a,8),h=h+g|0,u=Z(u^h,7),o=o+d|0,_=Z(_^o,16),f=f+_|0,d=Z(d^f,12),o=o+d|0,_=Z(_^o,8),f=f+_|0,d=Z(d^f,7),s=s+c|0,v=Z(v^s,16),p=p+v|0,c=Z(c^p,12),s=s+c|0,v=Z(v^s,8),p=p+v|0,c=Z(c^p,7);let b=0;r[b++]=i,r[b++]=a,r[b++]=o,r[b++]=s,r[b++]=g,r[b++]=_,r[b++]=v,r[b++]=y}var pt=st(dt,{counterRight:!1,counterLength:8,extendNonceFn:ft,allowShortKeys:!1}),mt=new Uint8Array(16),ht=(e,t)=>{e.update(t);let n=t.length%16;n&&e.update(mt.subarray(n))},gt=new Uint8Array(32);function _t(e,t,n,r,i){let a=e(t,n,gt),o=ut.create(a);i&&ht(o,i),ht(o,r);let s=qe(r.length,i?i.length:0,!0);o.update(s);let c=o.digest();return Le(a,s),c}var vt=We({blockSize:64,nonceLength:24,tagLength:16},(e=>(t,n,r)=>({encrypt(i,a){let o=i.length;a=Ge(o+16,a,!1),a.set(i);let s=a.subarray(0,-16);e(t,n,s,s,1);let c=_t(e,t,n,s,r);return a.set(c,o),Le(c),a},decrypt(i,a){a=Ge(i.length-16,a,!1);let o=i.subarray(0,-16),s=i.subarray(-16),c=_t(e,t,n,o,r);if(!Ue(s,c))throw Error(`invalid tag`);return a.set(i.subarray(0,-16)),e(t,n,a,a,1),Le(c),a}}))(pt)),yt=typeof globalThis==`object`&&`crypto`in globalThis?globalThis.crypto:void 0;function bt(e=32){if(yt&&typeof yt.getRandomValues==`function`)return yt.getRandomValues(new Uint8Array(e));if(yt&&typeof yt.randomBytes==`function`)return Uint8Array.from(yt.randomBytes(e));throw Error(`crypto.getRandomValues must be defined`)}function xt(e){return je(e)}function St(e){let t=e.substring(0,32);return new Uint8Array(t.split(``).map(e=>e.charCodeAt(0)))}function Ct(e,t){let n=bt(24),r=new Uint8Array(32);r.set(e.slice(0,32));let i=new TextEncoder().encode(t),a=vt(r,n).encrypt(i),o=new Uint8Array(n.length+a.length);return o.set(n,0),o.set(a,n.length),o}function wt(e,t){let n=t.slice(0,24),r=t.slice(24),i=new Uint8Array(32);i.set(e.slice(0,32));let a=vt(i,n).decrypt(r);return new TextDecoder().decode(a)}function Tt(e){return new Uint8Array(e)}function Et(e){return e}function Dt(e){return btoa(String.fromCharCode(...e))}var Ot=`123456789abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ`,kt=()=>{let e=crypto.getRandomValues(new Uint8Array(16));e[6]=e[6]&15|64,e[8]=e[8]&63|128;let t=jt(e).toLowerCase();return`${t.substring(0,8)}-${t.substring(8,12)}-${t.substring(12,16)}-${t.substring(16,20)}-${t.substring(20)}`},At=()=>{let e=It(`0123456789abcdef`,Ot,kt().toLowerCase().replace(/-/g,``)),t=Math.ceil(Math.log(2**128)/Math.log(58));return e.padStart(t,Ot[0])},jt=e=>{let t=``;for(let n=0;n<e.length;n++)t+=e[n].toString(16).padStart(2,`0`);return t},Mt=e=>jt(new Uint8Array(e)),Nt=e=>/[\u3400-\u9FBF]/.test(e);function Pt(e){return e?btoa(encodeURIComponent(e)):``}function Ft(e){try{return decodeURIComponent(atob(e))}catch{return e}}function It(e,t,n){let r=[],i=e.length,a=t.length,o,s,c,l=n.length,u=``;if(e===t)return n;for(o=0;o<l;o++)r[o]=e.indexOf(n[o]);do{for(s=0,c=0,o=0;o<l;o++)s=s*i+r[o],s>=a?(r[c++]=parseInt((s/a).toString(),10),s%=a):c>0&&(r[c++]=0);l=c,u=t.slice(s,s+1).concat(u)}while(c!==0);return u}var Lt=/(\b(((https?|ftp):\/\/)|www.)[A-Z0-9+&@#\/%?=~_|!:,.;-]*[-A-Z0-9+&@#\/%=~_|])/gim,Rt=/(\w+@[a-zA-Z_]+?\.[a-zA-Z]{2,6})/gim;function zt(e){let t=Bt(e);return t=t.replace(Lt,`<a href="$1" target="_blank">$1</a>`),t=t.replace(Rt,`<a href="mailto:$1">$1</a>`),t.replace(/\n\r?/g,`<br />`)}function Bt(e){return e.replace(/[\u00A0-\u9999<>&'"]/gim,function(e){return`&#`+e.charCodeAt(0)+`;`})}function Vt(e){return e.replace(/!\[.*?\]\(.*?\)|!\[.*?\]\[.*?\]|<img.*?>/gi,`🖼`).replace(`
`,``).replace(/^\s*/,``)}function Ht(e){let t=e.toLowerCase();return t.startsWith(`https://`)||t.startsWith(`http://`)||t.startsWith(`blob:`)}function Ut(e,t=``){return Ht(e)?e:F(`/fs?id=${encodeURIComponent(e)}${t}`)}function Wt(e,t){return!t||!e?``:Ut($t(e,t))}var Gt=new Set;function Kt(e,t){if(Gt.has(e))return;Gt.add(e),setTimeout(()=>Gt.delete(e),1e3);let n=document.createElement(`a`);typeof n.download==`string`?(n.href=e,n.download=t,document.body.appendChild(n),n.click(),document.body.removeChild(n)):window.open(e)}function qt(e,t,n){let r=new Blob([e],{type:t});Kt(URL.createObjectURL(r),n)}function Jt(e){return e?e.substring(e.lastIndexOf(`/`)+1):``}function Yt(e){return Uint8Array.from(atob(e),e=>e.charCodeAt(0))}function Xt(e,t){return e?Dt(Ct(e,t)):``}function Zt(e,t,n,r=``){if(!e||!t?.ip||!t?.port||!n)return``;let i=Xt(e,`https://${t.ip}:${t.port}/fs?id=${encodeURIComponent(n)}${r}`);return`${B()}/proxyfs?id=${encodeURIComponent(i)}`}function Qt(e,t){if(t.startsWith(`app://`))return e+`/`+t.replace(`app://`,``);if(t.startsWith(`fid:`)){let n=t.replace(`fid:`,``);return`${e}/${n.substring(0,2)}/${n.substring(2,4)}/${n}`}return t}function $t(e,t,n=``){if(!t||!e)return``;let r=t.toLowerCase();if(r.startsWith(`https://`)||r.startsWith(`http://`))return t;let i=window.fileIdMap||new Map;if(i.has(t))return i.get(t)??``;let a=Dt(Ct(e,n?JSON.stringify({path:t,mediaId:n}):t));return i.set(t,a),a}function en(e){let t=e.lastIndexOf(`.`);return t===-1||e.lastIndexOf(`/`)>t?``:e.substring(t+1).toLowerCase()}async function tn(e,t={},n){return window.__TAURI_INTERNALS__.invoke(e,t,n)}var nn=[`chat`],rn=[`home`,`developer`,...nn];function an(e){return nn.includes(e)}function on(e){return typeof e==`string`&&rn.includes(e)}function sn(){return!1}var cn=window.__SERVER_TIME__?window.__SERVER_TIME__-Date.now():0;function ln(){return console.log(`Server time offset: ${cn} ms`),Date.now()+cn}function un(){let e=new Uint8Array(16);return crypto.getRandomValues(e),jt(e)}function dn(e){return`${ln()}|${un()}|${e}`}var fn=3e4,pn=new Map;async function mn(e,t){let n=JSON.stringify({query:e,variables:t}),r=pn.get(n);if(r)return r;let i=hn(e,t);pn.set(n,i);try{return await i}finally{pn.delete(n)}}async function hn(e,t){let n=`${B()}/graphql`,r=Yt(y()?P():E()),i=JSON.stringify({query:e,variables:t});console.info(`[request] ${i}`);let a=performance.now(),o=Et(Ct(r,dn(i))),s=performance.now(),c=new AbortController,l=setTimeout(()=>c.abort(),fn);try{let e=await fetch(n,{method:`POST`,headers:{...R()},body:o,signal:c.signal});if(e.status===401)throw y()||(D(),window.location.reload()),new gn(`unauthorized`,401);if(e.status===403)throw new gn(`web_access_disabled`,403);let t=await e.arrayBuffer(),i=performance.now(),l=wt(r,Tt(t)),u=performance.now();return console.info(`[response] ${l}`),console.info(`[time] encrypt: ${s-a}ms, api: ${i-s}ms, decrypt: ${u-i}ms`),JSON.parse(l)}catch(e){throw e instanceof gn?e:e.name===`AbortError`?new gn(`connection_timeout`):new gn(e.message||`network_error`)}finally{clearTimeout(l)}}var gn=class extends Error{status;constructor(e,t){super(e),this.status=t,this.name=`GqlError`}},_n=`
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
`,vn=`
  fragment PlaylistAudioFragment on PlaylistAudio {
    title
    artist
    path
    duration
  }
`,yn=`
  fragment AppFragment on App {
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
  ${vn}
`,bn=`
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
`,xn=`
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
`,Sn=`
  fragment MessageConversationFragment on MessageConversation {
    id
    address
    snippet
    date
    messageCount
    read
  }
`,Cn=`
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
`,wn=`
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
`,Tn=`
  fragment FileFragment on File {
    path
    isDir
    createdAt
    updatedAt
    size
    children
    mediaId
  }
`,En=`
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
`,Dn=`
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
`,On=`
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
`,kn=`
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
`,An=`
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
`,jn=`
  fragment FeedFragment on Feed {
    id
    name
    url
    fetchContent
    createdAt
    updatedAt
  }
`,Mn=`
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
`,Nn=`
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
`,Pn=`
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
`,Fn=`
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
`,In=`
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
`,Ln=`
  fragment BookmarkGroupFragment on BookmarkGroup {
    id
    name
    collapsed
    sortOrder
    createdAt
    updatedAt
  }
`,Rn=`
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

`;function zn(e){return e instanceof gn?e.status===403?`web_access_disabled`:e.message:`network_error`}function Bn(e){if(e)return typeof e==`function`?e():e}function Vn(o){let s=a(!1),c=a();async function l(e){s.value=!0;try{let t=e??Bn(o.variables),n=await mn(o.document,t);n.errors?.length?o.handle(n.data,n.errors[0].message):(c.value=n.data,o.handle(n.data,``))}catch(e){o.handle(void 0,zn(e))}finally{s.value=!1}}if(l(),typeof o.variables==`function`){let a=!0;e()&&(i(()=>{a=!1}),t(()=>{a=!0})),r(o.variables,async()=>{await n(),a&&l()},{deep:!0})}return{loading:s,result:c,refetch:l}}function Hn(e){let t=a(!1),n=a();async function r(r){t.value=!0;try{let t=r??Bn(e.variables),i=await mn(e.document,t);i.errors?.length?e.handle(i.data,i.errors[0].message):(n.value=i.data,e.handle(i.data,``))}catch(t){e.handle(void 0,zn(t))}finally{t.value=!1}}return{loading:t,result:n,fetch:r}}var Un=`
  query ($id: String!) {
    chatItems(id: $id) {
      ...ChatItemFragment
    }
  }
  ${bn}
`,Wn=`
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
`,Gn=`
  query {
    latestChatItems {
      ...ChatItemFragment
    }
  }
  ${bn}
`,Kn=`
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
`,qn=`
  query {
    chatChannels {
      ...ChatChannelFragment
    }
  }
  ${Rn}
`,Jn=`
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
`,Yn=`
  query sms($offset: Int!, $limit: Int!, $query: String!) {
    sms(offset: $offset, limit: $limit, query: $query) {
      ...MessageFragment
    }
    smsCount(query: $query)
  }
  ${xn}
`,Xn=`
  query {
    sims {
      id
      label
      number
      subscriptionId
    }
  }
`,Zn=`
  query smsConversations($offset: Int!, $limit: Int!, $query: String!) {
    smsConversations(offset: $offset, limit: $limit, query: $query) {
      ...MessageConversationFragment
    }
    smsConversationCount(query: $query)
  }
  ${Sn}
`,Qn=`
  query contacts($offset: Int!, $limit: Int!, $query: String!) {
    contacts(offset: $offset, limit: $limit, query: $query) {
      ...ContactFragment
    }
    contactCount(query: $query)
  }
  ${Cn}
`,$n=`
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
`,er=`
  query {
    contactSources {
      name
      type
    }
  }
`,tr=`
  query calls($offset: Int!, $limit: Int!, $query: String!) {
    calls(offset: $offset, limit: $limit, query: $query) {
      ...CallFragment
    }
    callCount(query: $query)
  }
  ${wn}
`,nr=`
  query images($offset: Int!, $limit: Int!, $query: String!, $sortBy: FileSortBy!) {
    images(offset: $offset, limit: $limit, query: $query, sortBy: $sortBy) {
      ...ImageFragment
    }
    imageCount(query: $query)
  }
  ${En}
`,rr=`
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
`,ir=`
  query videos($offset: Int!, $limit: Int!, $query: String!, $sortBy: FileSortBy!) {
    videos(offset: $offset, limit: $limit, query: $query, sortBy: $sortBy) {
      ...VideoFragment
    }
    videoCount(query: $query)
  }
  ${Dn}
`,ar=`
  query audios($offset: Int!, $limit: Int!, $query: String!, $sortBy: FileSortBy!) {
    items: audios(offset: $offset, limit: $limit, query: $query, sortBy: $sortBy) {
      ...AudioFragment
    }
    total: audioCount(query: $query)
  }
  ${On}
`,or=`
  query files($root: String!, $offset: Int!, $limit: Int!, $query: String!, $sortBy: FileSortBy!) {
    files(root: $root, offset: $offset, limit: $limit, query: $query, sortBy: $sortBy) {
      ...FileFragment
    }
  }
  ${Tn}
`,sr=`
  query recentFiles {
    recentFiles {
      ...FileFragment
    }
  }
  ${Tn}
`,cr=`
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
`,lr=`
  query {
    app {
      ...AppFragment
    }
  }
  ${yn}
`,ur=`
  query tags($type: DataType!) {
    tags(type: $type) {
      ...TagFragment
    }
  }
  ${_n}
`,dr=`
  query mediaBuckets($type: DataType!) {
    mediaBuckets(type: $type) {
      id
      name
      itemCount
      topItems
    }
  }
`,fr=`
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
`,pr=`
  query note($id: ID!) {
    note(id: $id) {
      ...NoteFragment
    }
  }
  ${kn}
`,mr=`
  query {
    feeds {
      ...FeedFragment
    }
  }
  ${jn}
`,hr=`
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
`,gr=`
  query feedsTags($type: DataType!) {
    tags(type: $type) {
      ...TagFragment
    }
    feeds {
      ...FeedFragment
    }
  }
  ${jn}
  ${_n}
`,_r=`
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
  ${_n}
`,vr=`
  query feedEntry($id: ID!) {
    feedEntry(id: $id) {
      ...FeedEntryFragment
      feed {
        ...FeedFragment
      }
    }
  }
  ${jn}
  ${Mn}
`,yr=`
  query imageCount($query: String!) {
    total: imageCount(query: $query)
    trash: imageCount(query: "trash:true")
  }
`,br=`
  query audioCount($query: String!) {
    total: audioCount(query: $query)
    trash: audioCount(query: "trash:true")
  }
`,xr=`
  query docs($offset: Int!, $limit: Int!, $query: String!, $sortBy: FileSortBy!) {
    items: docs(offset: $offset, limit: $limit, query: $query, sortBy: $sortBy) {
      ...DocFragment
    }
    total: docCount(query: $query)
  }
  ${An}
`,Sr=`
  query docCount($query: String!) {
    total: docCount(query: $query)
    trash: docCount(query: "trash:true")
    extGroups: docExtGroups {
      ext
      count
    }
  }
`,Cr=`
  query videoCount($query: String!) {
    total: videoCount(query: $query)
    trash: videoCount(query: "trash:true")
  }
`,wr=`
  query {
    total: packageCount(query: "")
    system: packageCount(query: "type:system")
  }
`,Tr=`
  query {
    total: feedEntryCount(query: "")
    today: feedEntryCount(query: "today:true")
    feedsCount {
      id
      count
    }
  }
`,Er=`
  query {
    total: contactCount(query: "")
  }
`,Dr=`
  query {
    total: callCount(query: "")
    incoming: callCount(query: "type:1")
    outgoing: callCount(query: "type:2")
    missed: callCount(query: "type:3")
  }
`,Or=`
  query {
    smsAllCounts {
      total
      inbox
      sent
      drafts
    }
  }
`,kr=`
  query {
    archivedConversations {
      ...MessageConversationFragment
    }
  }
  ${Sn}
`,Ar=`
  query {
    total: noteCount(query: "")
    trash: noteCount(query: "trash:true")
  }
`,jr=`
  query packages($offset: Int!, $limit: Int!, $query: String!, $sortBy: FileSortBy!) {
    packages(offset: $offset, limit: $limit, query: $query, sortBy: $sortBy) {
      ...PackageFragment
    }
    packageCount(query: $query)
  }
  ${Nn}
`,Mr=`
  query packageStatuses($ids: [ID!]!) {
    packageStatuses(ids: $ids) {
      id
      exist
      updatedAt
    }
  }
`,Nr=`
  query {
    screenMirrorState
    screenMirrorControlEnabled
    screenMirrorQuality {
      mode
      resolution
    }
  }
`,Pr=`
  query {
    screenMirrorControlEnabled
  }
`,Fr=`
  query {
    notifications {
      ...NotificationFragment
    }
  }
  ${Pn}
`,Ir=`
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
  ${Fn}
`,Lr=`
  query AppLogs($offset: Int!, $limit: Int!) {
    appLogs(offset: $offset, limit: $limit)
  }
`,Rr=`
  query {
    appLogPath
  }
`,zr=`
  query {
    dbPath
  }
`,Br=`
  query {
    dataStorePath
  }
`,Vr=`
  query uploadedChunks($fileId: String!) {
    uploadedChunks(fileId: $fileId)
  }
`,Hr=`
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
`,Ur=`
  query {
    dataStoreEntries {
      key
      value
    }
  }
`,Wr=`
  query {
    dbTables
  }
`,Gr=`
  query DbTableRowCount($table: String!) {
    dbTableRowCount(table: $table)
  }
`,Kr=`
  query DbTableRows($table: String!, $offset: Int!, $limit: Int!) {
    dbTableRows(table: $table, offset: $offset, limit: $limit)
  }
`,qr=`
  query {
    bookmarks {
      ...BookmarkFragment
    }
    bookmarkGroups {
      ...BookmarkGroupFragment
    }
  }
  ${In}
  ${Ln}
`;export{Nr as $,P as $t,gr as A,Jt as At,dr as B,Ft as Bt,Ir as C,on as Ct,Tr as D,Xt as Dt,hr as E,qt as Et,rr as F,Ht as Ft,Fr as G,Et as Gt,Ar as H,Vt as Ht,nr as I,Yt as It,jr as J,St as Jt,wr as K,wt as Kt,Hn as L,zt as Lt,or as M,Wt as Mt,$n as N,Qt as Nt,vr as O,en as Ot,yr as P,Zt as Pt,Pr as Q,R as Qt,Vn as R,Mt as Rt,Wr as S,sn as St,xr as T,Kt as Tt,pr as U,kt as Ut,cr as V,Pt as Vt,fr as W,At as Wt,Hr as X,j as Xt,Wn as Y,xt as Yt,sr as Z,B as Zt,Ur as _,vn as _t,kr as a,E as an,Vr as at,Gr as b,mn as bt,qr as c,_ as cn,In as ct,tr as d,x as dn,bn as dt,M as en,Xn as et,qn as f,y as fn,Cn as ft,Qn as g,kn as gt,er as h,Tn as ht,Lr as i,A as in,ur as it,Jn as j,Ut as jt,mr as k,$t as kt,_r as l,h as ln,Ln as lt,Er as m,jn as mt,lr as n,I as nn,Or as nt,br as o,O as on,Cr as ot,Un as p,g as pn,Mn as pt,Mr as q,Ct as qt,Rr as r,z as rn,Yn as rt,ar as s,S as sn,ir as st,Kn as t,V as tn,Zn as tt,Dr as u,v as un,Rn as ut,Br as v,_n as vt,Sr as w,tn as wt,Kr as x,an as xt,zr as y,gn as yt,Gn as z,Nt as zt};