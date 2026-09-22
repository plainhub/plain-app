import{C as e}from"./chunk-reactivity.esm-bundler-B13GM2Wg.js";import{r as t}from"./chunk-VDropdown-35juquAP.js";import{r as n,t as r}from"./chunk-gql-client-XsOpZ0w8.js";import{At as i,Ct as a,Et as o,Mt as s,Ot as c,Pt as l,St as u,Tt as d,jt as f,kt as ee,wt as p}from"./chunk-query-CILrbQwH.js";function te(i,a=!0){let o=e(!1),s=[],c=[];async function l(e){o.value=!0;try{let o=await n(i.document,e,{dedupe:!1});if(o.errors?.length){let e=o.errors[0].message;a&&t.emit(`toast`,e);let n=new r(e);for(let e of c)e(n);return}for(let e of s)e(o);return o}catch(e){let n=e instanceof r?e.message:`network_error`;a&&t.emit(`toast`,n);for(let t of c)t(e);return}finally{o.value=!1}}function u(e){return s.push(e),{off:()=>{let t=s.indexOf(e);t>=0&&s.splice(t,1)}}}function d(e){return c.push(e),{off:()=>{let t=c.indexOf(e);t>=0&&c.splice(t,1)}}}return{mutate:l,loading:o,onDone:u,onError:d}}async function m(e,t){return await e(t)!=null}var h=`
  mutation {
    clearAppLogs
  }
`,g=`
  mutation updateDeviceName($name: String!) {
    updateDeviceName(name: $name)
  }
`,_=`
  mutation sendChatItem($target: String!, $content: String!) {
    sendChatItem(target: $target, content: $content) {
      ...ChatItemFragment
    }
  }
  ${o}
`,v=`
  mutation deleteChatItem($id: ID!) {
    deleteChatItem(id: $id)
  }
`,y=`
  mutation deleteChatItems($query: String!) {
    deleteChatItems(query: $query) {
      affectedCount
    }
  }
`,b=`
  mutation retryChatItem($id: ID!) {
    retryChatItem(id: $id) {
      ...ChatItemFragment
    }
  }
  ${o}
`,x=`
  mutation createChatChannel($name: String!) {
    createChatChannel(name: $name) {
      ...ChatChannelFragment
    }
  }
  ${d}
`,S=`
  mutation updateChatChannel($id: ID!, $name: String!) {
    updateChatChannel(id: $id, name: $name) {
      ...ChatChannelFragment
    }
  }
  ${d}
`,C=`
  mutation deleteChatChannel($id: ID!) {
    deleteChatChannel(id: $id)
  }
`,w=`
  mutation deletePeer($id: ID!) {
    deletePeer(id: $id)
  }
`,T=`
  mutation unpairPeer($id: ID!) {
    unpairPeer(id: $id)
  }
`,E=`
  mutation leaveChatChannel($id: ID!) {
    leaveChatChannel(id: $id)
  }
`,D=`
  mutation addChatChannelMember($id: ID!, $peerId: String!) {
    addChatChannelMember(id: $id, peerId: $peerId) {
      ...ChatChannelFragment
    }
  }
  ${d}
`,O=`
  mutation removeChatChannelMember($id: ID!, $peerId: String!) {
    removeChatChannelMember(id: $id, peerId: $peerId) {
      ...ChatChannelFragment
    }
  }
  ${d}
`,k=`
  mutation respondChannelInvite($id: ID!, $accept: Boolean!) {
    respondChannelInvite(id: $id, accept: $accept)
  }
`,A=`
  mutation createDir($path: String!) {
    createDir(path: $path) {
      ...FileFragment
    }
  }
  ${f}
`,j=`
  mutation writeTextFile($path: String!, $content: String!, $overwrite: Boolean!) {
    writeTextFile(path: $path, content: $content, overwrite: $overwrite) {
      ...FileFragment
    }
  }
  ${f}
`,M=`
  mutation renameFile($path: String!, $name: String!) {
    renameFile(path: $path, name: $name)
  }
`,N=`
  mutation copyFile($src: String!, $dst: String!, $overwrite: Boolean!) {
    copyFile(src: $src, dst: $dst, overwrite: $overwrite)
  }
`,P=`
  mutation moveFile($src: String!, $dst: String!, $overwrite: Boolean!) {
    moveFile(src: $src, dst: $dst, overwrite: $overwrite)
  }
`,F=`
  mutation playAudio($path: String!) {
    playAudio(path: $path) {
      ...AudioItemFragment
    }
  }
  ${u}
`,I=`
  mutation updateAudioPlayMode($mode: MediaPlayMode!) {
    updateAudioPlayMode(mode: $mode)
  }
`,L=`
  mutation removeAudioFromQueue($path: String!) {
    removeAudioFromQueue(path: $path)
  }
`,R=`
  mutation addAudiosToQueue($query: String!) {
    addAudiosToQueue(query: $query)
  }
`,z=`
  mutation clearAudioQueue {
    clearAudioQueue
  }
`,B=`
  mutation reorderAudioQueue($paths: [String!]!) {
    reorderAudioQueue(paths: $paths)
  }
`,V=`
  mutation deleteMediaItems($type: MediaDataType!, $query: String!) {
    deleteMediaItems(type: $type, query: $query) {
      affectedCount
    }
  }
`,H=`
  mutation trashMediaItems($type: MediaDataType!, $query: String!) {
    trashMediaItems(type: $type, query: $query) {
      affectedCount
    }
  }
`,U=`
  mutation restoreMediaItems($type: MediaDataType!, $query: String!) {
    restoreMediaItems(type: $type, query: $query) {
      affectedCount
    }
  }
`,W=`
  mutation moveMediaItems($type: MediaDataType!, $query: String!, $destDir: String!) {
    moveMediaItems(type: $type, query: $query, destDir: $destDir) {
      affectedCount
    }
  }
`,G=`
  mutation trashSms($query: String!) {
    trashSms(query: $query) {
      affectedCount
    }
  }
`,K=`
  mutation restoreSms($query: String!) {
    restoreSms(query: $query) {
      affectedCount
    }
  }
`,q=`
  mutation deleteSms($query: String!) {
    deleteSms(query: $query) {
      affectedCount
    }
  }
`,J=`
  mutation removeFromTags($type: DataType!, $tagIds: [ID!]!, $query: String!) {
    removeFromTags(type: $type, tagIds: $tagIds, query: $query)
  }
`,Y=`
  mutation addToTags($type: DataType!, $tagIds: [ID!]!, $query: String!) {
    addToTags(type: $type, tagIds: $tagIds, query: $query)
  }
`,X=`
  mutation updateTagRelations($type: DataType!, $item: TagRelationStub!, $addTagIds: [ID!]!, $removeTagIds: [ID!]!) {
    updateTagRelations(type: $type, item: $item, addTagIds: $addTagIds, removeTagIds: $removeTagIds)
  }
`,Z=`
  mutation createTag($type: DataType!, $name: String!) {
    createTag(type: $type, name: $name) {
      ...TagFragment
    }
  }
  ${l}
`,Q=`
  mutation updateTag($id: ID!, $name: String!) {
    updateTag(id: $id, name: $name) {
      ...TagFragment
    }
  }
  ${l}
`,ne=`
  mutation deleteTag($id: ID!) {
    deleteTag(id: $id)
  }
`,re=`
  mutation addFavoriteFolder($rootPath: String!, $fullPath: String!) {
    addFavoriteFolder(rootPath: $rootPath, fullPath: $fullPath) {
      rootPath
      fullPath
    }
  }
`,ie=`
  mutation removeFavoriteFolder($fullPath: String!) {
    removeFavoriteFolder(fullPath: $fullPath) {
      rootPath
      fullPath
      alias
    }
  }
`,ae=`
  mutation setFavoriteFolderAlias($fullPath: String!, $alias: String!) {
    setFavoriteFolderAlias(fullPath: $fullPath, alias: $alias) {
      rootPath
      fullPath
      alias
    }
  }
`,oe=`
  mutation createNote($input: NoteInput!) {
    createNote(input: $input) {
      ...NoteFragment
    }
  }
  ${s}
`,se=`
  mutation updateNote($id: ID!, $input: NoteInput!) {
    updateNote(id: $id, input: $input) {
      ...NoteFragment
    }
  }
  ${s}
`,ce=`
  mutation deleteNotes($query: String!) {
    deleteNotes(query: $query) {
      affectedCount
    }
  }
`,le=`
  mutation trashNotes($query: String!) {
    trashNotes(query: $query) {
      affectedCount
    }
  }
`,ue=`
  mutation restoreNotes($query: String!) {
    restoreNotes(query: $query) {
      affectedCount
    }
  }
`,de=`
  mutation deleteFeedEntries($query: String!) {
    deleteFeedEntries(query: $query) {
      affectedCount
    }
  }
`,fe=`
  mutation deleteCalls($query: String!) {
    deleteCalls(query: $query) {
      affectedCount
    }
  }
`,pe=`
  mutation deleteContacts($query: String!) {
    deleteContacts(query: $query) {
      affectedCount
    }
  }
`,me=`
  mutation createFeed($url: String!, $fetchContent: Boolean!) {
    createFeed(url: $url, fetchContent: $fetchContent) {
      ...FeedFragment
    }
  }
  ${i}
`,he=`
  mutation importFeeds($content: String!) {
    importFeeds(content: $content)
  }
`,ge=`
  mutation exportFeeds {
    exportFeeds
  }
`,_e=`
  mutation exportNotes($query: String!) {
    exportNotes(query: $query)
  }
`,ve=`
  mutation relaunchApp {
    relaunchApp
  }
`,ye=`
  mutation openAccessibilitySettings {
    openAccessibilitySettings
  }
`,be=`
  mutation openWebSettings($feature: WebSettingsFeature) {
    openWebSettings(feature: $feature)
  }
`,xe=`
  mutation deleteFeed($id: ID!) {
    deleteFeed(id: $id)
  }
`,Se=`
  mutation updateFeed($id: ID!, $name: String!, $fetchContent: Boolean!) {
    updateFeed(id: $id, name: $name, fetchContent: $fetchContent) {
      ...FeedFragment
    }
  }
  ${i}
`,Ce=`
  mutation syncFeeds($id: ID) {
    syncFeeds(id: $id)
  }
`,we=`
  mutation syncFeedContent($id: ID!) {
    syncFeedContent(id: $id) {
      ...FeedEntryFragment
      feed {
        ...FeedFragment
      }
    }
  }
  ${i}
  ${ee}
`,Te=`
  mutation call($number: String!, $showDialer: Boolean!) {
    call(number: $number, showDialer: $showDialer)
  }
`,Ee=`
  mutation sendSms($number: String!, $body: String!, $subscriptionId: Int!) {
    sendSms(number: $number, body: $body, subscriptionId: $subscriptionId)
  }
`,De=`
  mutation sendSms($number: String!, $body: String!, $subscriptionId: Int!, $requestId: String!) {
    sendSms(number: $number, body: $body, subscriptionId: $subscriptionId, requestId: $requestId)
  }
`,Oe=`
  mutation archiveConversation($id: String!) {
    archiveConversation(id: $id)
  }
`,ke=`
  mutation unarchiveConversation($id: String!) {
    unarchiveConversation(id: $id)
  }
`,Ae=`
  mutation sendMms($number: String!, $body: String!, $attachmentPaths: [String!]!, $threadId: ID!) {
    sendMms(number: $number, body: $body, attachmentPaths: $attachmentPaths, threadId: $threadId)
  }
`,je=`
  mutation uninstallPackages($id: ID!) {
    uninstallPackages(ids: [$id])
  }
`,Me=`
  mutation installPackage($path: String!) {
    installPackage(path: $path) {
      id
      updatedAt
      isNew
    }
  }
`,Ne=`
  mutation startScreenMirror($audio: Boolean!) {
    startScreenMirror(audio: $audio)
  }
`,Pe=`
  mutation requestScreenMirrorAudio {
    requestScreenMirrorAudio
  }
`,Fe=`
  mutation stopScreenMirror {
    stopScreenMirror
  }
`,Ie=`
  mutation setTempValue($key: String!, $value: String!) {
    setTempValue(key: $key, value: $value) {
      key
      value
    }
  }
`,Le=`
  mutation updateScreenMirrorQuality($mode: ScreenMirrorMode!) {
    updateScreenMirrorQuality(mode: $mode)
  }
`,Re=`
  mutation saveFeedEntriesToNotes($query: String!) {
    saveFeedEntriesToNotes(query: $query)
  }
`,ze=`
  mutation mergeChunks($fileId: String!, $totalChunks: Int!, $path: String!, $replace: Boolean!, $totalSize: Long!) {
    mergeChunks(fileId: $fileId, totalChunks: $totalChunks, path: $path, replace: $replace, totalSize: $totalSize) {
      status
      value
      mergedSize
      error
    }
  }
`,Be=`
  mutation mergeAppFileChunks($fileId: String!, $totalChunks: Int!, $fileName: String!, $totalSize: Long!) {
    mergeAppFileChunks(fileId: $fileId, totalChunks: $totalChunks, fileName: $fileName, totalSize: $totalSize) {
      status
      value
      mergedSize
      error
    }
  }
`,Ve=`
  mutation deleteChunks($fileId: String!) {
    deleteChunks(fileId: $fileId)
  }
`,He=`
  mutation sendScreenMirrorControl($input: ScreenMirrorControlInput!) {
    sendScreenMirrorControl(input: $input)
  }
`,Ue=`
  mutation addBookmarks($urls: [String!]!, $groupId: ID!) {
    addBookmarks(urls: $urls, groupId: $groupId) {
      ...BookmarkFragment
    }
  }
  ${a}
`,We=`
  mutation updateBookmark($id: ID!, $input: BookmarkInput!) {
    updateBookmark(id: $id, input: $input) {
      ...BookmarkFragment
    }
  }
  ${a}
`,Ge=`
  mutation deleteBookmarks($ids: [ID!]!) {
    deleteBookmarks(ids: $ids) {
      affectedCount
    }
  }
`,Ke=`
  mutation recordBookmarkClick($id: ID!) {
    recordBookmarkClick(id: $id)
  }
`,qe=`
  mutation createBookmarkGroup($name: String!) {
    createBookmarkGroup(name: $name) {
      ...BookmarkGroupFragment
    }
  }
  ${p}
`,Je=`
  mutation updateBookmarkGroup($id: ID!, $name: String!, $collapsed: Boolean!, $sortOrder: Int!) {
    updateBookmarkGroup(id: $id, name: $name, collapsed: $collapsed, sortOrder: $sortOrder) {
      ...BookmarkGroupFragment
    }
  }
  ${p}
`,Ye=`
  mutation deleteBookmarkGroup($id: ID!) {
    deleteBookmarkGroup(id: $id)
  }
`,Xe=`
  mutation deleteFiles($paths: [String!]!) {
    deleteFiles(paths: $paths) {
      affectedCount
    }
  }
`,Ze=`
  mutation { enableImageSearch }
`,Qe=`
  mutation { disableImageSearch }
`,$e=`
  mutation { cancelImageModelDownload }
`,et=`
  mutation startImageIndex($force: Boolean) {
    startImageIndex(force: $force)
  }
`,tt=`
  mutation { cancelImageIndex }
`,nt=`
  mutation createContact($input: ContactInput!) {
    createContact(input: $input) {
      ...ContactFragment
    }
  }
  ${c}
`,rt=`
  mutation updateContact($id: ID!, $input: ContactInput!) {
    updateContact(id: $id, input: $input) {
      ...ContactFragment
    }
  }
  ${c}
`,it=`
  mutation DeleteNote($query: String!) {
    deleteNotes(query: $query)
  }
`,at=`
  mutation deleteFeedEntry($query: String!) {
    deleteFeedEntries(query: $query)
  }
`,ot=`
  mutation DeleteDataStoreEntry($key: String!) {
    deleteDataStoreEntry(key: $key)
  }
`,st=`
  mutation DeleteDbTableRows($table: String!, $ids: [String!]!) {
    deleteDbTableRows(table: $table, ids: $ids)
  }
`,ct=`
  mutation {
    startDiscovery
  }
`,lt=`
  mutation {
    stopDiscovery
  }
`,ut=`
  mutation pairDevice($input: PairingDeviceInput!) {
    pairDevice(input: $input)
  }
`,dt=`
  mutation cancelPairing($deviceId: String!) {
    cancelPairing(deviceId: $deviceId)
  }
`,ft=`
  mutation respondToPairing($input: PairingRequestInput!, $accepted: Boolean!) {
    respondToPairing(input: $input, accepted: $accepted)
  }
`,$=`
  mutation downloadPeerFile($messageId: ID!, $peerId: ID!) {
    downloadPeerFile(messageId: $messageId, peerId: $peerId)
  }
`,pt=`
  mutation pauseDownload($messageId: ID!) {
    pauseDownload(messageId: $messageId)
  }
`,mt=`
  mutation resumeDownload($messageId: ID!, $peerId: ID!) {
    resumeDownload(messageId: $messageId, peerId: $peerId)
  }
`,ht=`
  mutation retryDownload($messageId: ID!, $peerId: ID!) {
    retryDownload(messageId: $messageId, peerId: $peerId)
  }
`,gt=`
  mutation pauseMediaScan { pauseMediaScan }
`,_t=`
  mutation resumeMediaScan { resumeMediaScan }
`,vt=`
  mutation stopMediaScan { stopMediaScan }
`,yt=`
  mutation rebuildMediaIndex($root: String!) { rebuildMediaIndex(root: $root) }
`,bt=`
  mutation formatDisk($path: String!) {
    formatDisk(path: $path)
  }
`,xt=`
  mutation setSambaSettings($input: SambaSettingsInput!) {
    setSambaSettings(input: $input)
  }
`,St=`
  mutation setSambaUserPassword($password: String!) {
    setSambaUserPassword(password: $password)
  }
`;export{P as $,Je as $t,st as A,He as At,ne as B,lt as Bt,fe as C,_t as Ct,Ve as D,Re as Dt,y as E,m as Et,V as F,St as Ft,_e as G,H as Gt,$ as H,Fe as Ht,it as I,Ie as It,te as J,ke as Jt,bt as K,le as Kt,ce as L,ct as Lt,at as M,De as Mt,xe as N,ae as Nt,pe as O,_ as Ot,Xe as P,xt as Pt,ze as Q,We as Qt,w as R,et as Rt,Ge as S,mt as St,v as T,ht as Tt,Ze as U,we as Ut,Qe as V,vt as Vt,ge as W,Ce as Wt,E as X,T as Xt,Me as Y,je as Yt,Be as Z,I as Zt,A as _,k as _t,Y as a,Le as an,gt as at,Z as b,ue as bt,tt as c,j as cn,Ke as ct,h as d,O as dt,S as en,W as et,z as f,ie as ft,nt as g,Pe as gt,x as h,B as ht,re as i,se as in,pt as it,de as j,Ee as jt,ot as k,Ae as kt,$e as l,ve as lt,qe as m,M as mt,Ue as n,g as nn,be as nt,Oe as o,Q as on,F as ot,N as p,J as pt,he as q,G as qt,D as r,Se as rn,ut as rt,Te as s,X as sn,yt as st,R as t,rt as tn,ye as tt,dt as u,L as ut,me as v,ft as vt,C as w,b as wt,Ye as x,K as xt,oe as y,U as yt,q as z,Ne as zt};