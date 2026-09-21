import{C as e}from"./reactivity.esm-bundler-B13GM2Wg.js";import{s as t}from"./VDropdown-CVSf2RK8.js";import{r as n,t as r}from"./gql-client-DStlKFhS.js";import{Ct as i,Dt as a,Et as o,Ot as s,St as c,Tt as l,bt as u,jt as d,kt as f,xt as p,yt as ee}from"./query-aJGXH5aA.js";function te(i,a=!0){let o=e(!1),s=[],c=[];async function l(e){o.value=!0;try{let o=await n(i.document,e,{dedupe:!1});if(o.errors?.length){let e=o.errors[0].message;a&&t.emit(`toast`,e);let n=new r(e);for(let e of c)e(n);return}for(let e of s)e(o);return o}catch(e){let n=e instanceof r?e.message:`network_error`;a&&t.emit(`toast`,n);for(let t of c)t(e);return}finally{o.value=!1}}function u(e){return s.push(e),{off:()=>{let t=s.indexOf(e);t>=0&&s.splice(t,1)}}}function d(e){return c.push(e),{off:()=>{let t=c.indexOf(e);t>=0&&c.splice(t,1)}}}return{mutate:l,loading:o,onDone:u,onError:d}}async function m(e,t){return await e(t)!=null}var h=`
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
  ${i}
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
  ${i}
`,x=`
  mutation createChatChannel($name: String!) {
    createChatChannel(name: $name) {
      ...ChatChannelFragment
    }
  }
  ${c}
`,S=`
  mutation updateChatChannel($id: ID!, $name: String!) {
    updateChatChannel(id: $id, name: $name) {
      ...ChatChannelFragment
    }
  }
  ${c}
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
  ${c}
`,O=`
  mutation removeChatChannelMember($id: ID!, $peerId: String!) {
    removeChatChannelMember(id: $id, peerId: $peerId) {
      ...ChatChannelFragment
    }
  }
  ${c}
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
  ${s}
`,j=`
  mutation writeTextFile($path: String!, $content: String!, $overwrite: Boolean!) {
    writeTextFile(path: $path, content: $content, overwrite: $overwrite) {
      ...FileFragment
    }
  }
  ${s}
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
  ${ee}
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
  ${d}
`,Q=`
  mutation updateTag($id: ID!, $name: String!) {
    updateTag(id: $id, name: $name) {
      ...TagFragment
    }
  }
  ${d}
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
  ${f}
`,se=`
  mutation updateNote($id: ID!, $input: NoteInput!) {
    updateNote(id: $id, input: $input) {
      ...NoteFragment
    }
  }
  ${f}
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
  ${a}
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
  ${a}
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
  ${a}
  ${o}
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
  ${u}
`,We=`
  mutation updateBookmark($id: ID!, $input: BookmarkInput!) {
    updateBookmark(id: $id, input: $input) {
      ...BookmarkFragment
    }
  }
  ${u}
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
  ${l}
`,rt=`
  mutation updateContact($id: ID!, $input: ContactInput!) {
    updateContact(id: $id, input: $input) {
      ...ContactFragment
    }
  }
  ${l}
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
`;export{W as $,g as $t,st as A,Ee as At,ne as B,we as Bt,fe as C,b as Ct,Ve as D,_ as Dt,y as E,Re as Et,V as F,et as Ft,_e as G,ke as Gt,$ as H,H as Ht,it as I,Ne as It,Me as J,I as Jt,he as K,je as Kt,ce as L,lt as Lt,at as M,ae as Mt,xe as N,Ie as Nt,pe as O,Ae as Ot,Xe as P,ct as Pt,P as Q,rt as Qt,w as R,vt as Rt,Ge as S,_t as St,v as T,m as Tt,Ze as U,le as Ut,Qe as V,Ce as Vt,ge as W,G as Wt,Be as X,Je as Xt,E as Y,We as Yt,ze as Z,S as Zt,A as _,ft as _t,Y as a,j as an,F as at,Z as b,K as bt,tt as c,ve as ct,h as d,ie as dt,Se as en,ye as et,z as f,J as ft,nt as g,k as gt,x as h,Pe as ht,re as i,X as in,gt as it,de as j,De as jt,ot as k,He as kt,$e as l,L as lt,qe as m,B as mt,Ue as n,Le as nn,ut as nt,Oe as o,yt as ot,N as p,M as pt,te as q,T as qt,D as r,Q as rn,pt as rt,Te as s,Ke as st,R as t,se as tn,be as tt,dt as u,O as ut,me as v,U as vt,C as w,ht as wt,Ye as x,mt as xt,oe as y,ue as yt,q as z,Fe as zt};