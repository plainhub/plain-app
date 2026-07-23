import{A as e,P as t,Q as n,at as r,ct as i,dt as a,ft as o,g as s,it as c,k as l,nt as u,ot as d,pt as f,rt as p,st as ee,tt as m}from"./vue.runtime.esm-bundler-DB7W0Wog.js";import{Ct as h,St as g,_t as _,bt as v,dt as y,fn as b,ft as x,gt as S,ht as C,mt as w,pn as T,pt as E,vt as D,xt as O,yt as k}from"./query-yg6gMeKv.js";function te(){let e=new Map;function t(t,n){let r=e.get(t);r?r.add(n):e.set(t,new Set([n]))}function n(t,n){n?e.get(t)?.delete(n):e.delete(t)}function r(t,n){e.get(t)?.forEach(e=>e(n))}return{on:t,off:n,emit:r}}var A=te(),ne=typeof window<`u`,j,M=e=>j=e,N=Symbol();function P(e){return e&&typeof e==`object`&&Object.prototype.toString.call(e)===`[object Object]`&&typeof e.toJSON!=`function`}var F;(function(e){e.direct=`direct`,e.patchObject=`patch object`,e.patchFunction=`patch function`})(F||={});var I=typeof window==`object`&&window.window===window?window:typeof self==`object`&&self.self===self?self:typeof global==`object`&&global.global===global?global:typeof globalThis==`object`?globalThis:{HTMLElement:null};function re(e,{autoBom:t=!1}={}){return t&&/^\s*(?:text\/\S*|application\/xml|\S*\/\S*\+xml)\s*;.*charset\s*=\s*utf-8/i.test(e.type)?new Blob([`﻿`,e],{type:e.type}):e}function L(e,t,n){let r=new XMLHttpRequest;r.open(`GET`,e),r.responseType=`blob`,r.onload=function(){ie(r.response,t,n)},r.onerror=function(){console.error(`could not download file`)},r.send()}function R(e){let t=new XMLHttpRequest;t.open(`HEAD`,e,!1);try{t.send()}catch{}return t.status>=200&&t.status<=299}function z(e){try{e.dispatchEvent(new MouseEvent(`click`))}catch{let t=new MouseEvent(`click`,{bubbles:!0,cancelable:!0,view:window,detail:0,screenX:80,screenY:20,clientX:80,clientY:20,ctrlKey:!1,altKey:!1,shiftKey:!1,metaKey:!1,button:0,relatedTarget:null});e.dispatchEvent(t)}}var B=typeof navigator==`object`?navigator:{userAgent:``},V=/Macintosh/.test(B.userAgent)&&/AppleWebKit/.test(B.userAgent)&&!/Safari/.test(B.userAgent),ie=ne?typeof HTMLAnchorElement<`u`&&`download`in HTMLAnchorElement.prototype&&!V?ae:`msSaveOrOpenBlob`in B?oe:se:()=>{};function ae(e,t=`download`,n){let r=document.createElement(`a`);r.download=t,r.rel=`noopener`,typeof e==`string`?(r.href=e,r.origin===location.origin?z(r):R(r.href)?L(e,t,n):(r.target=`_blank`,z(r))):(r.href=URL.createObjectURL(e),setTimeout(function(){URL.revokeObjectURL(r.href)},4e4),setTimeout(function(){z(r)},0))}function oe(e,t=`download`,n){if(typeof e==`string`)if(R(e))L(e,t,n);else{let t=document.createElement(`a`);t.href=e,t.target=`_blank`,setTimeout(function(){z(t)})}else navigator.msSaveOrOpenBlob(re(e,n),t)}function se(e,t,n,r){if(r||=open(``,`_blank`),r&&(r.document.title=r.document.body.innerText=`downloading...`),typeof e==`string`)return L(e,t,n);let i=e.type===`application/octet-stream`,a=/constructor/i.test(String(I.HTMLElement))||`safari`in I,o=/CriOS\/[\d]+/.test(navigator.userAgent);if((o||i&&a||V)&&typeof FileReader<`u`){let t=new FileReader;t.onloadend=function(){let e=t.result;if(typeof e!=`string`)throw r=null,Error(`Wrong reader.result type`);e=o?e:e.replace(/^data:[^;]*;/,`data:attachment/file;`),r?r.location.href=e:location.assign(e),r=null},t.readAsDataURL(e)}else{let t=URL.createObjectURL(e);r?r.location.assign(t):location.href=t,r=null,setTimeout(function(){URL.revokeObjectURL(t)},4e4)}}var{assign:ce}=Object;function le(){let e=m(!0),t=e.run(()=>i({})),n=[],a=[],o=r({install(e){M(o),o._a=e,e.provide(N,o),e.config.globalProperties.$pinia=o,a.forEach(e=>n.push(e)),a=[]},use(e){return this._a?n.push(e):a.push(e),this},_p:n,_a:null,_e:e,_s:new Map,state:t});return o}var H=()=>{};function U(e,t,n,r=H){e.add(t);let i=()=>{e.delete(t)&&r()};return!n&&u()&&d(i),i}function W(e,...t){e.forEach(e=>{e(...t)})}var ue=e=>e(),G=Symbol(),K=Symbol();function q(e,t){e instanceof Map&&t instanceof Map?t.forEach((t,n)=>e.set(n,t)):e instanceof Set&&t instanceof Set&&t.forEach(e.add,e);for(let n in t){if(!t.hasOwnProperty(n))continue;let r=t[n],i=e[n];P(i)&&P(r)&&e.hasOwnProperty(n)&&!c(r)&&!p(r)?e[n]=q(i,r):e[n]=r}return e}var de=Symbol();function fe(e){return!P(e)||!Object.prototype.hasOwnProperty.call(e,de)}var{assign:J}=Object;function pe(e){return!!(c(e)&&e.effect)}function me(e,t,n,i){let{state:a,actions:o,getters:c}=t,l=n.state.value[e],u;function d(){return l||(n.state.value[e]=a?a():{}),J(f(n.state.value[e]),o,Object.keys(c||{}).reduce((t,i)=>(t[i]=r(s(()=>{M(n);let t=n._s.get(e);return c[i].call(t,t)})),t),{}))}return u=Y(e,d,t,n,i,!0),u}function Y(e,r,o={},s,l,u){let d,f=J({actions:{}},o),h={deep:!0},g,_,v=new Set,y=new Set,b=s.state.value[e];!u&&!b&&(s.state.value[e]={}),i({});let x;function S(n){let r;g=_=!1,typeof n==`function`?(n(s.state.value[e]),r={type:F.patchFunction,storeId:e,events:void 0}):(q(s.state.value[e],n),r={type:F.patchObject,payload:n,storeId:e,events:void 0});let i=x=Symbol();t().then(()=>{x===i&&(g=!0)}),_=!0,W(v,r,s.state.value[e])}let C=u?function(){let{state:e}=o,t=e?e():{};this.$patch(e=>{J(e,t)})}:H;function w(){d.stop(),v.clear(),y.clear(),s._s.delete(e)}let T=(t,n=``)=>{if(G in t)return t[K]=n,t;let r=function(){M(s);let n=Array.from(arguments),i=new Set,a=new Set;function o(e){i.add(e)}function c(e){a.add(e)}W(y,{args:n,name:r[K],store:E,after:o,onError:c});let l;try{l=t.apply(this&&this.$id===e?this:E,n)}catch(e){throw W(a,e),e}return l instanceof Promise?l.then(e=>(W(i,e),e)).catch(e=>(W(a,e),Promise.reject(e))):(W(i,l),l)};return r[G]=!0,r[K]=n,r},E=ee({_p:s,$id:e,$onAction:U.bind(null,y),$patch:S,$reset:C,$subscribe(t,r={}){let i=U(v,t,r.detached,()=>a()),a=d.run(()=>n(()=>s.state.value[e],n=>{(r.flush===`sync`?_:g)&&t({storeId:e,type:F.direct,events:void 0},n)},J({},h,r)));return i},$dispose:w});s._s.set(e,E);let D=(s._a&&s._a.runWithContext||ue)(()=>s._e.run(()=>(d=m()).run(()=>r({action:T}))));for(let t in D){let n=D[t];c(n)&&!pe(n)||p(n)?u||(b&&fe(n)&&(c(n)?n.value=b[t]:q(n,b[t])),s.state.value[e][t]=n):typeof n==`function`&&(D[t]=T(n,t),f.actions[t]=n)}return J(E,D),J(a(E),D),Object.defineProperty(E,"$state",{get:()=>s.state.value[e],set:e=>{S(t=>{J(t,e)})}}),s._p.forEach(e=>{J(E,d.run(()=>e({store:E,app:s._a,pinia:s,options:f})))}),b&&u&&o.hydrate&&o.hydrate(E.$state,b),g=!0,_=!0,E}function X(t,n,r){let i,a=typeof n==`function`;i=a?r:n;function o(r,o){let s=l();return r||=s?e(N,null):null,r&&M(r),r=j,r._s.has(t)||(a?Y(t,n,i,r):me(t,i,r)),r._s.get(t)}return o.$id=t,o}function he(e){let t=a(e),n={};for(let r in t){let i=t[r];i.effect?n[r]=s({get:()=>e[r],set(t){e[r]=t}}):(c(i)||p(i))&&(n[r]=o(e,r))}return n}function ge(e,t=!0){let n=i(!1),r=[],a=[];async function o(i){n.value=!0;try{let n=await h(e.document,i);if(n.errors?.length){let e=n.errors[0].message;t&&A.emit(`toast`,e);let r=new g(e);for(let e of a)e(r);return}for(let e of r)e(n);return n}catch(e){let n=e instanceof g?e.message:`network_error`;t&&A.emit(`toast`,n);for(let t of a)t(e);return}finally{n.value=!1}}function s(e){return r.push(e),{off:()=>{let t=r.indexOf(e);t>=0&&r.splice(t,1)}}}function c(e){return a.push(e),{off:()=>{let t=a.indexOf(e);t>=0&&a.splice(t,1)}}}return{mutate:o,loading:n,onDone:s,onError:c}}async function _e(e,t){return await e(t)!=null}var ve=`
  mutation {
    clearAppLogs
  }
`,ye=`
  mutation updateDeviceName($name: String!) {
    updateDeviceName(name: $name)
  }
`,be=`
  mutation sendChatItem($toId: String!, $content: String!) {
    sendChatItem(toId: $toId, content: $content) {
      ...ChatItemFragment
    }
  }
  ${w}
`,xe=`
  mutation deleteChatItem($id: ID!) {
    deleteChatItem(id: $id)
  }
`,Se=`
  mutation deleteChatItems($query: String!) {
    deleteChatItems(query: $query)
  }
`,Ce=`
  mutation retryChatItem($id: ID!) {
    retryChatItem(id: $id) {
      ...ChatItemFragment
    }
  }
  ${w}
`,we=`
  mutation createChatChannel($name: String!) {
    createChatChannel(name: $name) {
      ...ChatChannelFragment
    }
  }
  ${E}
`,Te=`
  mutation updateChatChannel($id: ID!, $name: String!) {
    updateChatChannel(id: $id, name: $name) {
      ...ChatChannelFragment
    }
  }
  ${E}
`,Ee=`
  mutation deleteChatChannel($id: ID!) {
    deleteChatChannel(id: $id)
  }
`,De=`
  mutation deletePeer($id: ID!) {
    deletePeer(id: $id)
  }
`,Oe=`
  mutation unpairPeer($id: ID!) {
    unpairPeer(id: $id)
  }
`,ke=`
  mutation leaveChatChannel($id: ID!) {
    leaveChatChannel(id: $id)
  }
`,Ae=`
  mutation addChatChannelMember($id: ID!, $peerId: String!) {
    addChatChannelMember(id: $id, peerId: $peerId) {
      ...ChatChannelFragment
    }
  }
  ${E}
`,je=`
  mutation removeChatChannelMember($id: ID!, $peerId: String!) {
    removeChatChannelMember(id: $id, peerId: $peerId) {
      ...ChatChannelFragment
    }
  }
  ${E}
`,Me=`
  mutation respondChannelInvite($id: ID!, $accept: Boolean!) {
    respondChannelInvite(id: $id, accept: $accept)
  }
`,Ne=`
  mutation createDir($path: String!) {
    createDir(path: $path) {
      ...FileFragment
    }
  }
  ${D}
`,Pe=`
  mutation writeTextFile($path: String!, $content: String!, $overwrite: Boolean!) {
    writeTextFile(path: $path, content: $content, overwrite: $overwrite) {
      ...FileFragment
    }
  }
  ${D}
`,Fe=`
  mutation renameFile($path: String!, $name: String!) {
    renameFile(path: $path, name: $name)
  }
`,Ie=`
  mutation copyFile($src: String!, $dst: String!, $overwrite: Boolean!) {
    copyFile(src: $src, dst: $dst, overwrite: $overwrite)
  }
`,Le=`
  mutation moveFile($src: String!, $dst: String!, $overwrite: Boolean!) {
    moveFile(src: $src, dst: $dst, overwrite: $overwrite)
  }
`,Re=`
  mutation playAudio($path: String!) {
    playAudio(path: $path) {
      ...PlaylistAudioFragment
    }
  }
  ${v}
`,ze=`
  mutation updateAudioPlayMode($mode: MediaPlayMode!) {
    updateAudioPlayMode(mode: $mode)
  }
`,Be=`
  mutation deletePlaylistAudio($path: String!) {
    deletePlaylistAudio(path: $path)
  }
`,Ve=`
  mutation addPlaylistAudios($query: String!) {
    addPlaylistAudios(query: $query)
  }
`,He=`
  mutation clearAudioPlaylist {
    clearAudioPlaylist
  }
`,Ue=`
  mutation reorderPlaylistAudios($paths: [String!]!) {
    reorderPlaylistAudios(paths: $paths)
  }
`,We=`
  mutation deleteMediaItems($type: DataType!, $query: String!) {
    deleteMediaItems(type: $type, query: $query) {
      type
      query
    }
  }
`,Ge=`
  mutation trashMediaItems($type: DataType!, $query: String!) {
    trashMediaItems(type: $type, query: $query) {
      type
      query
    }
  }
`,Ke=`
  mutation restoreMediaItems($type: DataType!, $query: String!) {
    restoreMediaItems(type: $type, query: $query) {
      type
      query
    }
  }
`,qe=`
  mutation removeFromTags($type: DataType!, $tagIds: [ID!]!, $query: String!) {
    removeFromTags(type: $type, tagIds: $tagIds, query: $query)
  }
`,Je=`
  mutation addToTags($type: DataType!, $tagIds: [ID!]!, $query: String!) {
    addToTags(type: $type, tagIds: $tagIds, query: $query)
  }
`,Ye=`
  mutation updateTagRelations($type: DataType!, $item: TagRelationStub!, $addTagIds: [ID!]!, $removeTagIds: [ID!]!) {
    updateTagRelations(type: $type, item: $item, addTagIds: $addTagIds, removeTagIds: $removeTagIds)
  }
`,Xe=`
  mutation createTag($type: DataType!, $name: String!) {
    createTag(type: $type, name: $name) {
      ...TagFragment
    }
  }
  ${O}
`,Ze=`
  mutation updateTag($id: ID!, $name: String!) {
    updateTag(id: $id, name: $name) {
      ...TagFragment
    }
  }
  ${O}
`,Qe=`
  mutation deleteTag($id: ID!) {
    deleteTag(id: $id)
  }
`,$e=`
  mutation addFavoriteFolder($rootPath: String!, $fullPath: String!) {
    addFavoriteFolder(rootPath: $rootPath, fullPath: $fullPath) {
      rootPath
      fullPath
    }
  }
`,et=`
  mutation removeFavoriteFolder($fullPath: String!) {
    removeFavoriteFolder(fullPath: $fullPath) {
      rootPath
      fullPath
      alias
    }
  }
`,tt=`
  mutation setFavoriteFolderAlias($fullPath: String!, $alias: String!) {
    setFavoriteFolderAlias(fullPath: $fullPath, alias: $alias) {
      rootPath
      fullPath
      alias
    }
  }
`,nt=`
  mutation saveNote($id: ID!, $input: NoteInput!) {
    saveNote(id: $id, input: $input) {
      ...NoteFragment
    }
  }
  ${k}
`,rt=`
  mutation deleteNotes($query: String!) {
    deleteNotes(query: $query)
  }
`,it=`
  mutation trashNotes($query: String!) {
    trashNotes(query: $query)
  }
`,at=`
  mutation restoreNotes($query: String!) {
    restoreNotes(query: $query)
  }
`,ot=`
  mutation deleteFeedEntries($query: String!) {
    deleteFeedEntries(query: $query)
  }
`,st=`
  mutation deleteCalls($query: String!) {
    deleteCalls(query: $query)
  }
`,ct=`
  mutation deleteContacts($query: String!) {
    deleteContacts(query: $query)
  }
`,lt=`
  mutation createFeed($url: String!, $fetchContent: Boolean!) {
    createFeed(url: $url, fetchContent: $fetchContent) {
      ...FeedFragment
    }
  }
  ${_}
`,ut=`
  mutation importFeeds($content: String!) {
    importFeeds(content: $content)
  }
`,dt=`
  mutation exportFeeds {
    exportFeeds
  }
`,ft=`
  mutation exportNotes($query: String!) {
    exportNotes(query: $query)
  }
`,pt=`
  mutation relaunchApp {
    relaunchApp
  }
`,mt=`
  mutation openAccessibilitySettings {
    openAccessibilitySettings
  }
`,ht=`
  mutation openWebSettings {
    openWebSettings
  }
`,gt=`
  mutation deleteFeed($id: ID!) {
    deleteFeed(id: $id)
  }
`,_t=`
  mutation updateFeed($id: ID!, $name: String!, $fetchContent: Boolean!) {
    updateFeed(id: $id, name: $name, fetchContent: $fetchContent) {
      ...FeedFragment
    }
  }
  ${_}
`,vt=`
  mutation syncFeeds($id: ID) {
    syncFeeds(id: $id)
  }
`,yt=`
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
`,bt=`
  mutation call($number: String!, $showDialer: Boolean!) {
    call(number: $number, showDialer: $showDialer)
  }
`,xt=`
  mutation setClip($text: String!) {
    setClip(text: $text)
  }
`,St=`
  mutation sendSms($number: String!, $body: String!, $subscriptionId: Int!) {
    sendSms(number: $number, body: $body, subscriptionId: $subscriptionId)
  }
`,Ct=`
  mutation archiveConversation($id: String!, $date: Long!) {
    archiveConversation(id: $id, date: $date)
  }
`,wt=`
  mutation unarchiveConversation($id: String!) {
    unarchiveConversation(id: $id)
  }
`,Tt=`
  mutation sendMms($number: String!, $body: String!, $attachmentPaths: [String!]!, $threadId: String!) {
    sendMms(number: $number, body: $body, attachmentPaths: $attachmentPaths, threadId: $threadId)
  }
`,Et=`
  mutation uninstallPackages($id: ID!) {
    uninstallPackages(ids: [$id])
  }
`,Dt=`
  mutation installPackage($path: String!) {
    installPackage(path: $path) {
      packageName
      updatedAt
      isNew
    }
  }
`,Ot=`
  mutation startScreenMirror($audio: Boolean!) {
    startScreenMirror(audio: $audio)
  }
`,kt=`
  mutation requestScreenMirrorAudio {
    requestScreenMirrorAudio
  }
`,At=`
  mutation stopScreenMirror {
    stopScreenMirror
  }
`,jt=`
  mutation setTempValue($key: String!, $value: String!) {
    setTempValue(key: $key, value: $value) {
      key
      value
    }
  }
`,Mt=`
  mutation cancelNotifications($ids: [ID!]!) {
    cancelNotifications(ids: $ids)
  }
`,Nt=`
  mutation replyNotification($id: ID!, $actionIndex: Int!, $text: String!) {
    replyNotification(id: $id, actionIndex: $actionIndex, text: $text)
  }
`,Pt=`
  mutation updateScreenMirrorQuality($mode: ScreenMirrorMode!) {
    updateScreenMirrorQuality(mode: $mode)
  }
`,Ft=`
  mutation saveFeedEntriesToNotes($query: String!) {
    saveFeedEntriesToNotes(query: $query)
  }
`,It=`
  mutation mergeChunks($fileId: String!, $totalChunks: Int!, $path: String!, $replace: Boolean!, $isAppFile: Boolean!) {
    mergeChunks(fileId: $fileId, totalChunks: $totalChunks, path: $path, replace: $replace, isAppFile: $isAppFile)
  }
`,Lt=`
  mutation deleteChunks($fileId: String!) {
    deleteChunks(fileId: $fileId)
  }
`,Rt=`
  mutation startPomodoro($timeLeft: Int!) {
    startPomodoro(timeLeft: $timeLeft)
  }
`,zt=`
  mutation stopPomodoro {
    stopPomodoro
  }
`,Bt=`
  mutation pausePomodoro {
    pausePomodoro
  }
`,Vt=`
  mutation sendScreenMirrorControl($input: ScreenMirrorControlInput!) {
    sendScreenMirrorControl(input: $input)
  }
`,Ht=`
  mutation addBookmarks($urls: [String!]!, $groupId: String!) {
    addBookmarks(urls: $urls, groupId: $groupId) {
      ...BookmarkFragment
    }
  }
  ${y}
`,Ut=`
  mutation updateBookmark($id: ID!, $input: BookmarkInput!) {
    updateBookmark(id: $id, input: $input) {
      ...BookmarkFragment
    }
  }
  ${y}
`,Wt=`
  mutation deleteBookmarks($ids: [ID!]!) {
    deleteBookmarks(ids: $ids)
  }
`,Gt=`
  mutation recordBookmarkClick($id: ID!) {
    recordBookmarkClick(id: $id)
  }
`,Kt=`
  mutation createBookmarkGroup($name: String!) {
    createBookmarkGroup(name: $name) {
      ...BookmarkGroupFragment
    }
  }
  ${x}
`,qt=`
  mutation updateBookmarkGroup($id: ID!, $name: String!, $collapsed: Boolean!, $sortOrder: Int!) {
    updateBookmarkGroup(id: $id, name: $name, collapsed: $collapsed, sortOrder: $sortOrder) {
      ...BookmarkGroupFragment
    }
  }
  ${x}
`,Jt=`
  mutation deleteBookmarkGroup($id: ID!) {
    deleteBookmarkGroup(id: $id)
  }
`,Yt=`
  mutation deleteFiles($paths: [String!]!) {
    deleteFiles(paths: $paths)
  }
`,Xt=`
  mutation { enableImageSearch }
`,Zt=`
  mutation { disableImageSearch }
`,Qt=`
  mutation { cancelImageModelDownload }
`,$t=`
  mutation startImageIndex($force: Boolean) {
    startImageIndex(force: $force)
  }
`,en=`
  mutation { cancelImageIndex }
`,tn=`
  mutation createContact($input: ContactInput!) {
    createContact(input: $input) {
      ...ContactFragment
    }
  }
  ${C}
`,nn=`
  mutation updateContact($id: ID!, $input: ContactInput!) {
    updateContact(id: $id, input: $input) {
      ...ContactFragment
    }
  }
  ${C}
`,Z=`
  mutation DeleteNote($query: String!) {
    deleteNotes(query: $query)
  }
`,rn=`
  mutation deleteFeedEntry($query: String!) {
    deleteFeedEntries(query: $query)
  }
`,an=`
  mutation DeleteDataStoreEntry($key: String!) {
    deleteDataStoreEntry(key: $key)
  }
`,on=`
  mutation DeleteDbTableRows($table: String!, $ids: [String!]!) {
    deleteDbTableRows(table: $table, ids: $ids)
  }
`,sn=`
  mutation {
    startDiscovery
  }
`,cn=`
  mutation {
    stopDiscovery
  }
`,ln=`
  mutation pairDevice($input: PairingDeviceInput!) {
    pairDevice(input: $input)
  }
`,un=`
  mutation cancelPairing($deviceId: String!) {
    cancelPairing(deviceId: $deviceId)
  }
`,dn=`
  mutation respondToPairing($input: PairingRequestInput!, $accepted: Boolean!) {
    respondToPairing(input: $input, accepted: $accepted)
  }
`,fn=`plain-web:store:`,Q=new Map;function pn(e){let t=Q.get(e);if(t)return t;let n=new BroadcastChannel(fn+e),r=new Set;return n.onmessage=e=>{let t=e.data;if(!(!t||t.windowId===T())&&t.clientId===b())for(let e of r)e(t.patch)},t={bc:n,subscribers:r},Q.set(e,t),t}var $=globalThis.__plainWebInstalled??new WeakSet;globalThis.__plainWebInstalled=$;function mn(e,t,n){if($.has(e))return;$.add(e);let r=pn(t);r.subscribers.add(t=>{e.__cw_replaying=!0;try{e.$patch(t)}finally{queueMicrotask(()=>{e.__cw_replaying=!1})}}),e.$subscribe((t,i)=>{if(e.__cw_replaying)return;let o={};for(let e of n)o[e]=a(i[e]);let s=JSON.parse(JSON.stringify(o)),c={windowId:T(),clientId:b(),patch:s};r.bc.postMessage(c)},{detached:!0})}function hn(e,t,n){let r=X(e,t),{syncKeys:i}=n;return(()=>{let t=r();return mn(t,e,i),t})}var gn=hn(`temp`,{state:()=>({app:{clientId:``},urlTokenKey:null,uploads:[],selectedFiles:[],audioPlaying:!1,lightbox:{sources:[],visible:!1,index:-1},counter:{messages:-1,contacts:-1,calls:-1,videos:-1,videosTrash:-1,images:-1,imagesTrash:-1,audios:-1,audiosTrash:-1,packages:-1,packagesSystem:-1,notes:-1,notesTrash:-1,docs:-1,docsTrash:-1,docExtGroups:[],feedEntries:-1,feedEntriesToday:-1,total:-1,free:-1},feedsSyncing:!1})},{syncKeys:[`counter`,`audioPlaying`,`feedsSyncing`]});export{ht as $,X as $t,an as A,Ot as At,Be as B,Oe as Bt,Wt as C,St as Ct,Se as D,sn as Dt,xe as E,jt as Et,Yt as F,vt as Ft,ft as G,nn as Gt,Zt as H,Ut as Ht,We as I,Ge as It,Dt as J,Pt as Jt,ut as K,ye as Kt,Z as L,it as Lt,ot as M,zt as Mt,rn as N,At as Nt,Lt as O,$t as Ot,gt as P,yt as Pt,mt as Q,le as Qt,rt as R,wt as Rt,Jt as S,Vt as St,Ee as T,tt as Tt,Xt as U,qt as Ut,Qe as V,ze as Vt,dt as W,Te as Wt,It as X,Ye as Xt,ke as Y,Ze as Yt,Le as Z,Pe as Zt,we as _,_e as _t,Ve as a,je as at,lt as b,be as bt,bt as c,Fe as ct,Mt as d,kt as dt,he as en,ln as et,un as f,Me as ft,Kt as g,Ce as gt,Ie as h,at as ht,$e as i,pt as it,on as j,cn as jt,ct as k,Rt as kt,en as l,Ue as lt,He as m,Ke as mt,Ht as n,Re as nt,Je as o,et as ot,ve as p,dn as pt,ge as q,_t as qt,Ae as r,Gt as rt,Ct as s,qe as st,gn as t,A as tn,Bt as tt,Qt as u,Nt as ut,tn as v,Ft as vt,st as w,xt as wt,Xe as x,Tt as xt,Ne as y,nt as yt,De as z,Et as zt};