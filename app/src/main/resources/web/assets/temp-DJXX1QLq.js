import{A as e,P as t,Q as n,at as r,ct as i,dt as a,ft as o,g as s,it as c,k as l,nt as u,ot as d,pt as f,rt as p,st as ee,tt as m}from"./vue.runtime.esm-bundler-DB7W0Wog.js";import{Ct as h,St as g,_t as _,bt as v,dt as y,fn as b,ft as x,gt as S,ht as C,mt as w,pn as T,pt as E,vt as D,xt as O,yt as te}from"./query-Llv-VI-G.js";function ne(){let e=new Map;function t(t,n){let r=e.get(t);r?r.add(n):e.set(t,new Set([n]))}function n(t,n){n?e.get(t)?.delete(n):e.delete(t)}function r(t,n){e.get(t)?.forEach(e=>e(n))}return{on:t,off:n,emit:r}}var k=ne();function re(e,t=!0){let n=i(!1),r=[],a=[];async function o(i){n.value=!0;try{let n=await h(e.document,i);if(n.errors?.length){let e=n.errors[0].message;t&&k.emit(`toast`,e);let r=new g(e);for(let e of a)e(r);return}for(let e of r)e(n);return n}catch(e){let n=e instanceof g?e.message:`network_error`;t&&k.emit(`toast`,n);for(let t of a)t(e);return}finally{n.value=!1}}function s(e){return r.push(e),{off:()=>{let t=r.indexOf(e);t>=0&&r.splice(t,1)}}}function c(e){return a.push(e),{off:()=>{let t=a.indexOf(e);t>=0&&a.splice(t,1)}}}return{mutate:o,loading:n,onDone:s,onError:c}}async function ie(e,t){return await e(t)!=null}var ae=`
  mutation {
    clearAppLogs
  }
`,oe=`
  mutation updateDeviceName($name: String!) {
    updateDeviceName(name: $name)
  }
`,se=`
  mutation sendChatItem($toId: String!, $content: String!) {
    sendChatItem(toId: $toId, content: $content) {
      ...ChatItemFragment
    }
  }
  ${w}
`,ce=`
  mutation deleteChatItem($id: ID!) {
    deleteChatItem(id: $id)
  }
`,le=`
  mutation deleteChatItems($query: String!) {
    deleteChatItems(query: $query)
  }
`,ue=`
  mutation retryChatItem($id: ID!) {
    retryChatItem(id: $id) {
      ...ChatItemFragment
    }
  }
  ${w}
`,de=`
  mutation createChatChannel($name: String!) {
    createChatChannel(name: $name) {
      ...ChatChannelFragment
    }
  }
  ${E}
`,fe=`
  mutation updateChatChannel($id: ID!, $name: String!) {
    updateChatChannel(id: $id, name: $name) {
      ...ChatChannelFragment
    }
  }
  ${E}
`,pe=`
  mutation deleteChatChannel($id: ID!) {
    deleteChatChannel(id: $id)
  }
`,me=`
  mutation deletePeer($id: ID!) {
    deletePeer(id: $id)
  }
`,he=`
  mutation leaveChatChannel($id: ID!) {
    leaveChatChannel(id: $id)
  }
`,ge=`
  mutation addChatChannelMember($id: ID!, $peerId: String!) {
    addChatChannelMember(id: $id, peerId: $peerId) {
      ...ChatChannelFragment
    }
  }
  ${E}
`,_e=`
  mutation removeChatChannelMember($id: ID!, $peerId: String!) {
    removeChatChannelMember(id: $id, peerId: $peerId) {
      ...ChatChannelFragment
    }
  }
  ${E}
`,ve=`
  mutation respondChannelInvite($id: ID!, $accept: Boolean!) {
    respondChannelInvite(id: $id, accept: $accept)
  }
`,ye=`
  mutation createDir($path: String!) {
    createDir(path: $path) {
      ...FileFragment
    }
  }
  ${D}
`,be=`
  mutation writeTextFile($path: String!, $content: String!, $overwrite: Boolean!) {
    writeTextFile(path: $path, content: $content, overwrite: $overwrite) {
      ...FileFragment
    }
  }
  ${D}
`,xe=`
  mutation renameFile($path: String!, $name: String!) {
    renameFile(path: $path, name: $name)
  }
`,Se=`
  mutation copyFile($src: String!, $dst: String!, $overwrite: Boolean!) {
    copyFile(src: $src, dst: $dst, overwrite: $overwrite)
  }
`,Ce=`
  mutation moveFile($src: String!, $dst: String!, $overwrite: Boolean!) {
    moveFile(src: $src, dst: $dst, overwrite: $overwrite)
  }
`,A=`
  mutation playAudio($path: String!) {
    playAudio(path: $path) {
      ...PlaylistAudioFragment
    }
  }
  ${v}
`,we=`
  mutation updateAudioPlayMode($mode: MediaPlayMode!) {
    updateAudioPlayMode(mode: $mode)
  }
`,Te=`
  mutation deletePlaylistAudio($path: String!) {
    deletePlaylistAudio(path: $path)
  }
`,Ee=`
  mutation addPlaylistAudios($query: String!) {
    addPlaylistAudios(query: $query)
  }
`,De=`
  mutation clearAudioPlaylist {
    clearAudioPlaylist
  }
`,Oe=`
  mutation reorderPlaylistAudios($paths: [String!]!) {
    reorderPlaylistAudios(paths: $paths)
  }
`,ke=`
  mutation deleteMediaItems($type: DataType!, $query: String!) {
    deleteMediaItems(type: $type, query: $query) {
      type
      query
    }
  }
`,Ae=`
  mutation trashMediaItems($type: DataType!, $query: String!) {
    trashMediaItems(type: $type, query: $query) {
      type
      query
    }
  }
`,je=`
  mutation restoreMediaItems($type: DataType!, $query: String!) {
    restoreMediaItems(type: $type, query: $query) {
      type
      query
    }
  }
`,Me=`
  mutation removeFromTags($type: DataType!, $tagIds: [ID!]!, $query: String!) {
    removeFromTags(type: $type, tagIds: $tagIds, query: $query)
  }
`,Ne=`
  mutation addToTags($type: DataType!, $tagIds: [ID!]!, $query: String!) {
    addToTags(type: $type, tagIds: $tagIds, query: $query)
  }
`,Pe=`
  mutation updateTagRelations($type: DataType!, $item: TagRelationStub!, $addTagIds: [ID!]!, $removeTagIds: [ID!]!) {
    updateTagRelations(type: $type, item: $item, addTagIds: $addTagIds, removeTagIds: $removeTagIds)
  }
`,Fe=`
  mutation createTag($type: DataType!, $name: String!) {
    createTag(type: $type, name: $name) {
      ...TagFragment
    }
  }
  ${O}
`,Ie=`
  mutation updateTag($id: ID!, $name: String!) {
    updateTag(id: $id, name: $name) {
      ...TagFragment
    }
  }
  ${O}
`,Le=`
  mutation deleteTag($id: ID!) {
    deleteTag(id: $id)
  }
`,Re=`
  mutation addFavoriteFolder($rootPath: String!, $fullPath: String!) {
    addFavoriteFolder(rootPath: $rootPath, fullPath: $fullPath) {
      rootPath
      fullPath
    }
  }
`,ze=`
  mutation removeFavoriteFolder($fullPath: String!) {
    removeFavoriteFolder(fullPath: $fullPath) {
      rootPath
      fullPath
      alias
    }
  }
`,Be=`
  mutation setFavoriteFolderAlias($fullPath: String!, $alias: String!) {
    setFavoriteFolderAlias(fullPath: $fullPath, alias: $alias) {
      rootPath
      fullPath
      alias
    }
  }
`,Ve=`
  mutation saveNote($id: ID!, $input: NoteInput!) {
    saveNote(id: $id, input: $input) {
      ...NoteFragment
    }
  }
  ${te}
`,He=`
  mutation deleteNotes($query: String!) {
    deleteNotes(query: $query)
  }
`,Ue=`
  mutation trashNotes($query: String!) {
    trashNotes(query: $query)
  }
`,We=`
  mutation restoreNotes($query: String!) {
    restoreNotes(query: $query)
  }
`,Ge=`
  mutation deleteFeedEntries($query: String!) {
    deleteFeedEntries(query: $query)
  }
`,Ke=`
  mutation deleteCalls($query: String!) {
    deleteCalls(query: $query)
  }
`,qe=`
  mutation deleteContacts($query: String!) {
    deleteContacts(query: $query)
  }
`,Je=`
  mutation createFeed($url: String!, $fetchContent: Boolean!) {
    createFeed(url: $url, fetchContent: $fetchContent) {
      ...FeedFragment
    }
  }
  ${_}
`,Ye=`
  mutation importFeeds($content: String!) {
    importFeeds(content: $content)
  }
`,Xe=`
  mutation exportFeeds {
    exportFeeds
  }
`,Ze=`
  mutation exportNotes($query: String!) {
    exportNotes(query: $query)
  }
`,Qe=`
  mutation relaunchApp {
    relaunchApp
  }
`,$e=`
  mutation openAccessibilitySettings {
    openAccessibilitySettings
  }
`,et=`
  mutation openWebSettings {
    openWebSettings
  }
`,tt=`
  mutation deleteFeed($id: ID!) {
    deleteFeed(id: $id)
  }
`,nt=`
  mutation updateFeed($id: ID!, $name: String!, $fetchContent: Boolean!) {
    updateFeed(id: $id, name: $name, fetchContent: $fetchContent) {
      ...FeedFragment
    }
  }
  ${_}
`,rt=`
  mutation syncFeeds($id: ID) {
    syncFeeds(id: $id)
  }
`,it=`
  mutation syncFeedContent($id: ID!) {
    syncFeedContent(id: $id) {
      ...FeedEntryFragment
      feed {
        ...FeedFragment
      }
    }
  }
  ${_}
  ${S}
`,at=`
  mutation call($number: String!, $showDialer: Boolean!) {
    call(number: $number, showDialer: $showDialer)
  }
`,ot=`
  mutation setClip($text: String!) {
    setClip(text: $text)
  }
`,st=`
  mutation sendSms($number: String!, $body: String!, $subscriptionId: Int!) {
    sendSms(number: $number, body: $body, subscriptionId: $subscriptionId)
  }
`,ct=`
  mutation archiveConversation($id: String!, $date: Long!) {
    archiveConversation(id: $id, date: $date)
  }
`,lt=`
  mutation unarchiveConversation($id: String!) {
    unarchiveConversation(id: $id)
  }
`,ut=`
  mutation sendMms($number: String!, $body: String!, $attachmentPaths: [String!]!, $threadId: String!) {
    sendMms(number: $number, body: $body, attachmentPaths: $attachmentPaths, threadId: $threadId)
  }
`,dt=`
  mutation uninstallPackages($id: ID!) {
    uninstallPackages(ids: [$id])
  }
`,ft=`
  mutation installPackage($path: String!) {
    installPackage(path: $path) {
      packageName
      updatedAt
      isNew
    }
  }
`,pt=`
  mutation startScreenMirror($audio: Boolean!) {
    startScreenMirror(audio: $audio)
  }
`,mt=`
  mutation requestScreenMirrorAudio {
    requestScreenMirrorAudio
  }
`,ht=`
  mutation stopScreenMirror {
    stopScreenMirror
  }
`,gt=`
  mutation setTempValue($key: String!, $value: String!) {
    setTempValue(key: $key, value: $value) {
      key
      value
    }
  }
`,_t=`
  mutation cancelNotifications($ids: [ID!]!) {
    cancelNotifications(ids: $ids)
  }
`,vt=`
  mutation replyNotification($id: ID!, $actionIndex: Int!, $text: String!) {
    replyNotification(id: $id, actionIndex: $actionIndex, text: $text)
  }
`,yt=`
  mutation updateScreenMirrorQuality($mode: ScreenMirrorMode!) {
    updateScreenMirrorQuality(mode: $mode)
  }
`,bt=`
  mutation saveFeedEntriesToNotes($query: String!) {
    saveFeedEntriesToNotes(query: $query)
  }
`,xt=`
  mutation mergeChunks($fileId: String!, $totalChunks: Int!, $path: String!, $replace: Boolean!, $isAppFile: Boolean!) {
    mergeChunks(fileId: $fileId, totalChunks: $totalChunks, path: $path, replace: $replace, isAppFile: $isAppFile)
  }
`,St=`
  mutation deleteChunks($fileId: String!) {
    deleteChunks(fileId: $fileId)
  }
`,Ct=`
  mutation startPomodoro($timeLeft: Int!) {
    startPomodoro(timeLeft: $timeLeft)
  }
`,wt=`
  mutation stopPomodoro {
    stopPomodoro
  }
`,Tt=`
  mutation pausePomodoro {
    pausePomodoro
  }
`,Et=`
  mutation sendScreenMirrorControl($input: ScreenMirrorControlInput!) {
    sendScreenMirrorControl(input: $input)
  }
`,Dt=`
  mutation addBookmarks($urls: [String!]!, $groupId: String!) {
    addBookmarks(urls: $urls, groupId: $groupId) {
      ...BookmarkFragment
    }
  }
  ${y}
`,Ot=`
  mutation updateBookmark($id: ID!, $input: BookmarkInput!) {
    updateBookmark(id: $id, input: $input) {
      ...BookmarkFragment
    }
  }
  ${y}
`,kt=`
  mutation deleteBookmarks($ids: [ID!]!) {
    deleteBookmarks(ids: $ids)
  }
`,At=`
  mutation recordBookmarkClick($id: ID!) {
    recordBookmarkClick(id: $id)
  }
`,jt=`
  mutation createBookmarkGroup($name: String!) {
    createBookmarkGroup(name: $name) {
      ...BookmarkGroupFragment
    }
  }
  ${x}
`,Mt=`
  mutation updateBookmarkGroup($id: ID!, $name: String!, $collapsed: Boolean!, $sortOrder: Int!) {
    updateBookmarkGroup(id: $id, name: $name, collapsed: $collapsed, sortOrder: $sortOrder) {
      ...BookmarkGroupFragment
    }
  }
  ${x}
`,Nt=`
  mutation deleteBookmarkGroup($id: ID!) {
    deleteBookmarkGroup(id: $id)
  }
`,Pt=`
  mutation deleteFiles($paths: [String!]!) {
    deleteFiles(paths: $paths)
  }
`,Ft=`
  mutation { enableImageSearch }
`,It=`
  mutation { disableImageSearch }
`,Lt=`
  mutation { cancelImageModelDownload }
`,Rt=`
  mutation startImageIndex($force: Boolean) {
    startImageIndex(force: $force)
  }
`,zt=`
  mutation { cancelImageIndex }
`,Bt=`
  mutation createContact($input: ContactInput!) {
    createContact(input: $input) {
      ...ContactFragment
    }
  }
  ${C}
`,Vt=`
  mutation updateContact($id: ID!, $input: ContactInput!) {
    updateContact(id: $id, input: $input) {
      ...ContactFragment
    }
  }
  ${C}
`,Ht=`
  mutation DeleteNote($query: String!) {
    deleteNotes(query: $query)
  }
`,Ut=`
  mutation deleteFeedEntry($query: String!) {
    deleteFeedEntries(query: $query)
  }
`,Wt=`
  mutation DeleteDataStoreEntry($key: String!) {
    deleteDataStoreEntry(key: $key)
  }
`,Gt=`
  mutation DeleteDbTableRows($table: String!, $ids: [String!]!) {
    deleteDbTableRows(table: $table, ids: $ids)
  }
`,Kt=`
  mutation {
    startDiscovery
  }
`,qt=`
  mutation {
    stopDiscovery
  }
`,Jt=`
  mutation pairDevice($input: PairingDeviceInput!) {
    pairDevice(input: $input)
  }
`,Yt=`
  mutation cancelPairing($deviceId: String!) {
    cancelPairing(deviceId: $deviceId)
  }
`,Xt=`
  mutation respondToPairing($input: PairingRequestInput!, $accepted: Boolean!) {
    respondToPairing(input: $input, accepted: $accepted)
  }
`,Zt=typeof window<`u`,j,M=e=>j=e,N=Symbol();function P(e){return e&&typeof e==`object`&&Object.prototype.toString.call(e)===`[object Object]`&&typeof e.toJSON!=`function`}var F;(function(e){e.direct=`direct`,e.patchObject=`patch object`,e.patchFunction=`patch function`})(F||={});var I=typeof window==`object`&&window.window===window?window:typeof self==`object`&&self.self===self?self:typeof global==`object`&&global.global===global?global:typeof globalThis==`object`?globalThis:{HTMLElement:null};function Qt(e,{autoBom:t=!1}={}){return t&&/^\s*(?:text\/\S*|application\/xml|\S*\/\S*\+xml)\s*;.*charset\s*=\s*utf-8/i.test(e.type)?new Blob([`﻿`,e],{type:e.type}):e}function L(e,t,n){let r=new XMLHttpRequest;r.open(`GET`,e),r.responseType=`blob`,r.onload=function(){$t(r.response,t,n)},r.onerror=function(){console.error(`could not download file`)},r.send()}function R(e){let t=new XMLHttpRequest;t.open(`HEAD`,e,!1);try{t.send()}catch{}return t.status>=200&&t.status<=299}function z(e){try{e.dispatchEvent(new MouseEvent(`click`))}catch{let t=new MouseEvent(`click`,{bubbles:!0,cancelable:!0,view:window,detail:0,screenX:80,screenY:20,clientX:80,clientY:20,ctrlKey:!1,altKey:!1,shiftKey:!1,metaKey:!1,button:0,relatedTarget:null});e.dispatchEvent(t)}}var B=typeof navigator==`object`?navigator:{userAgent:``},V=/Macintosh/.test(B.userAgent)&&/AppleWebKit/.test(B.userAgent)&&!/Safari/.test(B.userAgent),$t=Zt?typeof HTMLAnchorElement<`u`&&`download`in HTMLAnchorElement.prototype&&!V?en:`msSaveOrOpenBlob`in B?tn:nn:()=>{};function en(e,t=`download`,n){let r=document.createElement(`a`);r.download=t,r.rel=`noopener`,typeof e==`string`?(r.href=e,r.origin===location.origin?z(r):R(r.href)?L(e,t,n):(r.target=`_blank`,z(r))):(r.href=URL.createObjectURL(e),setTimeout(function(){URL.revokeObjectURL(r.href)},4e4),setTimeout(function(){z(r)},0))}function tn(e,t=`download`,n){if(typeof e==`string`)if(R(e))L(e,t,n);else{let t=document.createElement(`a`);t.href=e,t.target=`_blank`,setTimeout(function(){z(t)})}else navigator.msSaveOrOpenBlob(Qt(e,n),t)}function nn(e,t,n,r){if(r||=open(``,`_blank`),r&&(r.document.title=r.document.body.innerText=`downloading...`),typeof e==`string`)return L(e,t,n);let i=e.type===`application/octet-stream`,a=/constructor/i.test(String(I.HTMLElement))||`safari`in I,o=/CriOS\/[\d]+/.test(navigator.userAgent);if((o||i&&a||V)&&typeof FileReader<`u`){let t=new FileReader;t.onloadend=function(){let e=t.result;if(typeof e!=`string`)throw r=null,Error(`Wrong reader.result type`);e=o?e:e.replace(/^data:[^;]*;/,`data:attachment/file;`),r?r.location.href=e:location.assign(e),r=null},t.readAsDataURL(e)}else{let t=URL.createObjectURL(e);r?r.location.assign(t):location.href=t,r=null,setTimeout(function(){URL.revokeObjectURL(t)},4e4)}}var{assign:rn}=Object;function an(){let e=m(!0),t=e.run(()=>i({})),n=[],a=[],o=r({install(e){M(o),o._a=e,e.provide(N,o),e.config.globalProperties.$pinia=o,a.forEach(e=>n.push(e)),a=[]},use(e){return this._a?n.push(e):a.push(e),this},_p:n,_a:null,_e:e,_s:new Map,state:t});return o}var H=()=>{};function U(e,t,n,r=H){e.add(t);let i=()=>{e.delete(t)&&r()};return!n&&u()&&d(i),i}function W(e,...t){e.forEach(e=>{e(...t)})}var on=e=>e(),G=Symbol(),K=Symbol();function q(e,t){e instanceof Map&&t instanceof Map?t.forEach((t,n)=>e.set(n,t)):e instanceof Set&&t instanceof Set&&t.forEach(e.add,e);for(let n in t){if(!t.hasOwnProperty(n))continue;let r=t[n],i=e[n];P(i)&&P(r)&&e.hasOwnProperty(n)&&!c(r)&&!p(r)?e[n]=q(i,r):e[n]=r}return e}var J=Symbol();function sn(e){return!P(e)||!Object.prototype.hasOwnProperty.call(e,J)}var{assign:Y}=Object;function cn(e){return!!(c(e)&&e.effect)}function ln(e,t,n,i){let{state:a,actions:o,getters:c}=t,l=n.state.value[e],u;function d(){return l||(n.state.value[e]=a?a():{}),Y(f(n.state.value[e]),o,Object.keys(c||{}).reduce((t,i)=>(t[i]=r(s(()=>{M(n);let t=n._s.get(e);return c[i].call(t,t)})),t),{}))}return u=X(e,d,t,n,i,!0),u}function X(e,r,o={},s,l,u){let d,f=Y({actions:{}},o),h={deep:!0},g,_,v=new Set,y=new Set,b=s.state.value[e];!u&&!b&&(s.state.value[e]={}),i({});let x;function S(n){let r;g=_=!1,typeof n==`function`?(n(s.state.value[e]),r={type:F.patchFunction,storeId:e,events:void 0}):(q(s.state.value[e],n),r={type:F.patchObject,payload:n,storeId:e,events:void 0});let i=x=Symbol();t().then(()=>{x===i&&(g=!0)}),_=!0,W(v,r,s.state.value[e])}let C=u?function(){let{state:e}=o,t=e?e():{};this.$patch(e=>{Y(e,t)})}:H;function w(){d.stop(),v.clear(),y.clear(),s._s.delete(e)}let T=(t,n=``)=>{if(G in t)return t[K]=n,t;let r=function(){M(s);let n=Array.from(arguments),i=new Set,a=new Set;function o(e){i.add(e)}function c(e){a.add(e)}W(y,{args:n,name:r[K],store:E,after:o,onError:c});let l;try{l=t.apply(this&&this.$id===e?this:E,n)}catch(e){throw W(a,e),e}return l instanceof Promise?l.then(e=>(W(i,e),e)).catch(e=>(W(a,e),Promise.reject(e))):(W(i,l),l)};return r[G]=!0,r[K]=n,r},E=ee({_p:s,$id:e,$onAction:U.bind(null,y),$patch:S,$reset:C,$subscribe(t,r={}){let i=U(v,t,r.detached,()=>a()),a=d.run(()=>n(()=>s.state.value[e],n=>{(r.flush===`sync`?_:g)&&t({storeId:e,type:F.direct,events:void 0},n)},Y({},h,r)));return i},$dispose:w});s._s.set(e,E);let D=(s._a&&s._a.runWithContext||on)(()=>s._e.run(()=>(d=m()).run(()=>r({action:T}))));for(let t in D){let n=D[t];c(n)&&!cn(n)||p(n)?u||(b&&sn(n)&&(c(n)?n.value=b[t]:q(n,b[t])),s.state.value[e][t]=n):typeof n==`function`&&(D[t]=T(n,t),f.actions[t]=n)}return Y(E,D),Y(a(E),D),Object.defineProperty(E,"$state",{get:()=>s.state.value[e],set:e=>{S(t=>{Y(t,e)})}}),s._p.forEach(e=>{Y(E,d.run(()=>e({store:E,app:s._a,pinia:s,options:f})))}),b&&u&&o.hydrate&&o.hydrate(E.$state,b),g=!0,_=!0,E}function Z(t,n,r){let i,a=typeof n==`function`;i=a?r:n;function o(r,o){let s=l();return r||=s?e(N,null):null,r&&M(r),r=j,r._s.has(t)||(a?X(t,n,i,r):ln(t,i,r)),r._s.get(t)}return o.$id=t,o}function un(e){let t=a(e),n={};for(let r in t){let i=t[r];i.effect?n[r]=s({get:()=>e[r],set(t){e[r]=t}}):(c(i)||p(i))&&(n[r]=o(e,r))}return n}var dn=`plain-web:store:`,Q=new Map;function fn(e){let t=Q.get(e);if(t)return t;let n=new BroadcastChannel(dn+e),r=new Set;return n.onmessage=e=>{let t=e.data;if(!(!t||t.windowId===T())&&t.clientId===b())for(let e of r)e(t.patch)},t={bc:n,subscribers:r},Q.set(e,t),t}var $=globalThis.__plainWebInstalled??new WeakSet;globalThis.__plainWebInstalled=$;function pn(e,t,n){if($.has(e))return;$.add(e);let r=fn(t);r.subscribers.add(t=>{e.__cw_replaying=!0;try{e.$patch(t)}finally{queueMicrotask(()=>{e.__cw_replaying=!1})}}),e.$subscribe((t,i)=>{if(e.__cw_replaying)return;let o={};for(let e of n)o[e]=a(i[e]);let s=JSON.parse(JSON.stringify(o)),c={windowId:T(),clientId:b(),patch:s};r.bc.postMessage(c)},{detached:!0})}function mn(e,t,n){let r=Z(e,t),{syncKeys:i}=n;return(()=>{let t=r();return pn(t,e,i),t})}var hn=mn(`temp`,{state:()=>({app:{clientId:``},urlTokenKey:null,uploads:[],selectedFiles:[],audioPlaying:!1,lightbox:{sources:[],visible:!1,index:-1},counter:{messages:-1,contacts:-1,calls:-1,videos:-1,videosTrash:-1,images:-1,imagesTrash:-1,audios:-1,audiosTrash:-1,packages:-1,packagesSystem:-1,notes:-1,notesTrash:-1,docs:-1,docsTrash:-1,docExtGroups:[],feedEntries:-1,feedEntriesToday:-1,total:-1,free:-1},feedsSyncing:!1})},{syncKeys:[`counter`,`audioPlaying`,`feedsSyncing`]});export{xt as $,be as $t,le as A,Kt as At,Ht as B,Ue as Bt,Je as C,se as Ct,Ke as D,ot as Dt,kt as E,st as Et,Ge as F,wt as Ft,It as G,Mt as Gt,me as H,dt as Ht,Ut as I,ht as It,Ze as J,oe as Jt,Ft as K,fe as Kt,tt as L,it as Lt,qe as M,Ct as Mt,Wt as N,pt as Nt,pe as O,Be as Ot,Gt as P,qt as Pt,he as Q,Pe as Qt,Pt as R,rt as Rt,ye as S,Ve as St,Nt as T,Et as Tt,Te as U,we as Ut,He as V,lt as Vt,Le as W,Ot as Wt,re as X,yt as Xt,Ye as Y,nt as Yt,ft as Z,Ie as Zt,De as _,je as _t,Dt as a,A as at,de as b,ie as bt,Ee as c,_e as ct,at as d,xe as dt,k as en,Ce as et,zt as f,Oe as ft,ae as g,Xt as gt,Yt as h,ve as ht,un as i,Tt as it,St as j,Rt as jt,ce as k,gt as kt,Ne as l,ze as lt,_t as m,mt,an as n,et as nt,ge as o,At as ot,Lt as p,vt as pt,Xe as q,Vt as qt,Z as r,Jt as rt,Re as s,Qe as st,hn as t,$e as tt,ct as u,Me as ut,Se as v,We as vt,Fe as w,ut as wt,Bt as x,bt as xt,jt as y,ue as yt,ke as z,Ae as zt};