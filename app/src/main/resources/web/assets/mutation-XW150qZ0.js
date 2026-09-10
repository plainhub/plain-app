import{vn as e}from"./runtime-core.esm-bundler-DrnlzXx3.js";import{s as t}from"./temp-BxyXOeiJ.js";import{r as n,t as r}from"./gql-client-Da0Gzuip.js";import{Ct as i,Dt as a,Et as o,St as s,_t as c,bt as l,gt as u,vt as d,wt as ee,xt as te,yt as f}from"./query-3j5Qtb97.js";function p(i,a=!0){let o=e(!1),s=[],c=[];async function l(e){o.value=!0;try{let o=await n(i.document,e,{dedupe:!1});if(o.errors?.length){let e=o.errors[0].message;a&&t.emit(`toast`,e);let n=new r(e);for(let e of c)e(n);return}for(let e of s)e(o);return o}catch(e){let n=e instanceof r?e.message:`network_error`;a&&t.emit(`toast`,n);for(let t of c)t(e);return}finally{o.value=!1}}function u(e){return s.push(e),{off:()=>{let t=s.indexOf(e);t>=0&&s.splice(t,1)}}}function d(e){return c.push(e),{off:()=>{let t=c.indexOf(e);t>=0&&c.splice(t,1)}}}return{mutate:l,loading:o,onDone:u,onError:d}}async function m(e,t){return await e(t)!=null}var h=`
  mutation {
    clearAppLogs
  }
`,g=`
  mutation updateDeviceName($name: String!) {
    updateDeviceName(name: $name)
  }
`,_=`
  mutation sendChatItem($toId: String!, $content: String!) {
    sendChatItem(toId: $toId, content: $content) {
      ...ChatItemFragment
    }
  }
  ${f}
`,v=`
  mutation deleteChatItem($id: ID!) {
    deleteChatItem(id: $id)
  }
`,y=`
  mutation deleteChatItems($query: String!) {
    deleteChatItems(query: $query)
  }
`,b=`
  mutation retryChatItem($id: ID!) {
    retryChatItem(id: $id) {
      ...ChatItemFragment
    }
  }
  ${f}
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
  ${i}
`,j=`
  mutation writeTextFile($path: String!, $content: String!, $overwrite: Boolean!) {
    writeTextFile(path: $path, content: $content, overwrite: $overwrite) {
      ...FileFragment
    }
  }
  ${i}
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
      ...PlaylistAudioFragment
    }
  }
  ${o}
`,I=`
  mutation updateAudioPlayMode($mode: MediaPlayMode!) {
    updateAudioPlayMode(mode: $mode)
  }
`,L=`
  mutation deletePlaylistAudio($path: String!) {
    deletePlaylistAudio(path: $path)
  }
`,R=`
  mutation addPlaylistAudios($query: String!) {
    addPlaylistAudios(query: $query)
  }
`,z=`
  mutation clearAudioPlaylist {
    clearAudioPlaylist
  }
`,B=`
  mutation reorderPlaylistAudios($paths: [String!]!) {
    reorderPlaylistAudios(paths: $paths)
  }
`,V=`
  mutation deleteMediaItems($type: DataType!, $query: String!) {
    deleteMediaItems(type: $type, query: $query) {
      type
      query
    }
  }
`,H=`
  mutation trashMediaItems($type: DataType!, $query: String!) {
    trashMediaItems(type: $type, query: $query) {
      type
      query
    }
  }
`,U=`
  mutation restoreMediaItems($type: DataType!, $query: String!) {
    restoreMediaItems(type: $type, query: $query) {
      type
      query
    }
  }
`,W=`
  mutation moveMediaItems($type: DataType!, $query: String!, $destDir: String!) {
    moveMediaItems(type: $type, query: $query, destDir: $destDir) {
      type
      query
    }
  }
`,G=`
  mutation removeFromTags($type: DataType!, $tagIds: [ID!]!, $query: String!) {
    removeFromTags(type: $type, tagIds: $tagIds, query: $query)
  }
`,K=`
  mutation addToTags($type: DataType!, $tagIds: [ID!]!, $query: String!) {
    addToTags(type: $type, tagIds: $tagIds, query: $query)
  }
`,q=`
  mutation updateTagRelations($type: DataType!, $item: TagRelationStub!, $addTagIds: [ID!]!, $removeTagIds: [ID!]!) {
    updateTagRelations(type: $type, item: $item, addTagIds: $addTagIds, removeTagIds: $removeTagIds)
  }
`,J=`
  mutation createTag($type: DataType!, $name: String!) {
    createTag(type: $type, name: $name) {
      ...TagFragment
    }
  }
  ${a}
`,Y=`
  mutation updateTag($id: ID!, $name: String!) {
    updateTag(id: $id, name: $name) {
      ...TagFragment
    }
  }
  ${a}
`,X=`
  mutation deleteTag($id: ID!) {
    deleteTag(id: $id)
  }
`,Z=`
  mutation addFavoriteFolder($rootPath: String!, $fullPath: String!) {
    addFavoriteFolder(rootPath: $rootPath, fullPath: $fullPath) {
      rootPath
      fullPath
    }
  }
`,Q=`
  mutation removeFavoriteFolder($fullPath: String!) {
    removeFavoriteFolder(fullPath: $fullPath) {
      rootPath
      fullPath
      alias
    }
  }
`,ne=`
  mutation setFavoriteFolderAlias($fullPath: String!, $alias: String!) {
    setFavoriteFolderAlias(fullPath: $fullPath, alias: $alias) {
      rootPath
      fullPath
      alias
    }
  }
`,re=`
  mutation saveNote($id: ID!, $input: NoteInput!) {
    saveNote(id: $id, input: $input) {
      ...NoteFragment
    }
  }
  ${ee}
`,ie=`
  mutation deleteNotes($query: String!) {
    deleteNotes(query: $query)
  }
`,ae=`
  mutation trashNotes($query: String!) {
    trashNotes(query: $query)
  }
`,oe=`
  mutation restoreNotes($query: String!) {
    restoreNotes(query: $query)
  }
`,se=`
  mutation deleteFeedEntries($query: String!) {
    deleteFeedEntries(query: $query)
  }
`,ce=`
  mutation deleteCalls($query: String!) {
    deleteCalls(query: $query)
  }
`,le=`
  mutation deleteContacts($query: String!) {
    deleteContacts(query: $query)
  }
`,ue=`
  mutation createFeed($url: String!, $fetchContent: Boolean!) {
    createFeed(url: $url, fetchContent: $fetchContent) {
      ...FeedFragment
    }
  }
  ${s}
`,de=`
  mutation importFeeds($content: String!) {
    importFeeds(content: $content)
  }
`,fe=`
  mutation exportFeeds {
    exportFeeds
  }
`,pe=`
  mutation exportNotes($query: String!) {
    exportNotes(query: $query)
  }
`,me=`
  mutation relaunchApp {
    relaunchApp
  }
`,he=`
  mutation openAccessibilitySettings {
    openAccessibilitySettings
  }
`,ge=`
  mutation openWebSettings {
    openWebSettings
  }
`,_e=`
  mutation deleteFeed($id: ID!) {
    deleteFeed(id: $id)
  }
`,ve=`
  mutation updateFeed($id: ID!, $name: String!, $fetchContent: Boolean!) {
    updateFeed(id: $id, name: $name, fetchContent: $fetchContent) {
      ...FeedFragment
    }
  }
  ${s}
`,ye=`
  mutation syncFeeds($id: ID) {
    syncFeeds(id: $id)
  }
`,be=`
  mutation syncFeedContent($id: ID!) {
    syncFeedContent(id: $id) {
      ...FeedEntryFragment
      feed {
        ...FeedFragment
      }
    }
  }
  ${s}
  ${te}
`,xe=`
  mutation call($number: String!, $showDialer: Boolean!) {
    call(number: $number, showDialer: $showDialer)
  }
`,Se=`
  mutation setClip($text: String!) {
    setClip(text: $text)
  }
`,Ce=`
  mutation sendSms($number: String!, $body: String!, $subscriptionId: Int!) {
    sendSms(number: $number, body: $body, subscriptionId: $subscriptionId)
  }
`,we=`
  mutation sendSms($number: String!, $body: String!, $subscriptionId: Int!, $requestId: String!) {
    sendSms(number: $number, body: $body, subscriptionId: $subscriptionId, requestId: $requestId)
  }
`,Te=`
  mutation archiveConversation($id: String!, $date: Long!) {
    archiveConversation(id: $id, date: $date)
  }
`,Ee=`
  mutation unarchiveConversation($id: String!) {
    unarchiveConversation(id: $id)
  }
`,De=`
  mutation sendMms($number: String!, $body: String!, $attachmentPaths: [String!]!, $threadId: String!) {
    sendMms(number: $number, body: $body, attachmentPaths: $attachmentPaths, threadId: $threadId)
  }
`,Oe=`
  mutation uninstallPackages($id: ID!) {
    uninstallPackages(ids: [$id])
  }
`,ke=`
  mutation installPackage($path: String!) {
    installPackage(path: $path) {
      packageName
      updatedAt
      isNew
    }
  }
`,Ae=`
  mutation startScreenMirror($audio: Boolean!) {
    startScreenMirror(audio: $audio)
  }
`,je=`
  mutation requestScreenMirrorAudio {
    requestScreenMirrorAudio
  }
`,Me=`
  mutation stopScreenMirror {
    stopScreenMirror
  }
`,Ne=`
  mutation setTempValue($key: String!, $value: String!) {
    setTempValue(key: $key, value: $value) {
      key
      value
    }
  }
`,Pe=`
  mutation cancelNotifications($ids: [ID!]!) {
    cancelNotifications(ids: $ids)
  }
`,Fe=`
  mutation replyNotification($id: ID!, $actionIndex: Int!, $text: String!) {
    replyNotification(id: $id, actionIndex: $actionIndex, text: $text)
  }
`,Ie=`
  mutation updateScreenMirrorQuality($mode: ScreenMirrorMode!) {
    updateScreenMirrorQuality(mode: $mode)
  }
`,Le=`
  mutation saveFeedEntriesToNotes($query: String!) {
    saveFeedEntriesToNotes(query: $query)
  }
`,Re=`
  mutation mergeChunks($fileId: String!, $totalChunks: Int!, $path: String!, $replace: Boolean!, $isAppFile: Boolean!, $totalSize: Long!) {
    mergeChunks(fileId: $fileId, totalChunks: $totalChunks, path: $path, replace: $replace, isAppFile: $isAppFile, totalSize: $totalSize)
  }
`,ze=`
  mutation mergeChunksAsync($fileId: String!, $totalChunks: Int!, $path: String!, $replace: Boolean!, $isAppFile: Boolean!, $totalSize: Long!) {
    mergeChunksAsync(fileId: $fileId, totalChunks: $totalChunks, path: $path, replace: $replace, isAppFile: $isAppFile, totalSize: $totalSize)
  }
`,Be=`
  mutation deleteChunks($fileId: String!) {
    deleteChunks(fileId: $fileId)
  }
`,Ve=`
  mutation startPomodoro($timeLeft: Int!) {
    startPomodoro(timeLeft: $timeLeft)
  }
`,He=`
  mutation stopPomodoro {
    stopPomodoro
  }
`,Ue=`
  mutation pausePomodoro {
    pausePomodoro
  }
`,We=`
  mutation sendScreenMirrorControl($input: ScreenMirrorControlInput!) {
    sendScreenMirrorControl(input: $input)
  }
`,Ge=`
  mutation addBookmarks($urls: [String!]!, $groupId: String!) {
    addBookmarks(urls: $urls, groupId: $groupId) {
      ...BookmarkFragment
    }
  }
  ${u}
`,Ke=`
  mutation updateBookmark($id: ID!, $input: BookmarkInput!) {
    updateBookmark(id: $id, input: $input) {
      ...BookmarkFragment
    }
  }
  ${u}
`,qe=`
  mutation deleteBookmarks($ids: [ID!]!) {
    deleteBookmarks(ids: $ids)
  }
`,Je=`
  mutation recordBookmarkClick($id: ID!) {
    recordBookmarkClick(id: $id)
  }
`,Ye=`
  mutation createBookmarkGroup($name: String!) {
    createBookmarkGroup(name: $name) {
      ...BookmarkGroupFragment
    }
  }
  ${c}
`,Xe=`
  mutation updateBookmarkGroup($id: ID!, $name: String!, $collapsed: Boolean!, $sortOrder: Int!) {
    updateBookmarkGroup(id: $id, name: $name, collapsed: $collapsed, sortOrder: $sortOrder) {
      ...BookmarkGroupFragment
    }
  }
  ${c}
`,Ze=`
  mutation deleteBookmarkGroup($id: ID!) {
    deleteBookmarkGroup(id: $id)
  }
`,Qe=`
  mutation deleteFiles($paths: [String!]!) {
    deleteFiles(paths: $paths)
  }
`,$e=`
  mutation { enableImageSearch }
`,et=`
  mutation { disableImageSearch }
`,tt=`
  mutation { cancelImageModelDownload }
`,$=`
  mutation startImageIndex($force: Boolean) {
    startImageIndex(force: $force)
  }
`,nt=`
  mutation { cancelImageIndex }
`,rt=`
  mutation createContact($input: ContactInput!) {
    createContact(input: $input) {
      ...ContactFragment
    }
  }
  ${l}
`,it=`
  mutation updateContact($id: ID!, $input: ContactInput!) {
    updateContact(id: $id, input: $input) {
      ...ContactFragment
    }
  }
  ${l}
`,at=`
  mutation DeleteNote($query: String!) {
    deleteNotes(query: $query)
  }
`,ot=`
  mutation deleteFeedEntry($query: String!) {
    deleteFeedEntries(query: $query)
  }
`,st=`
  mutation DeleteDataStoreEntry($key: String!) {
    deleteDataStoreEntry(key: $key)
  }
`,ct=`
  mutation DeleteDbTableRows($table: String!, $ids: [String!]!) {
    deleteDbTableRows(table: $table, ids: $ids)
  }
`,lt=`
  mutation {
    startDiscovery
  }
`,ut=`
  mutation {
    stopDiscovery
  }
`,dt=`
  mutation pairDevice($input: PairingDeviceInput!) {
    pairDevice(input: $input)
  }
`,ft=`
  mutation cancelPairing($deviceId: String!) {
    cancelPairing(deviceId: $deviceId)
  }
`,pt=`
  mutation respondToPairing($input: PairingRequestInput!, $accepted: Boolean!) {
    respondToPairing(input: $input, accepted: $accepted)
  }
`,mt=`
  mutation downloadPeerFile($messageId: ID!, $peerId: ID!) {
    downloadPeerFile(messageId: $messageId, peerId: $peerId)
  }
`,ht=`
  mutation pauseDownload($messageId: ID!) {
    pauseDownload(messageId: $messageId)
  }
`,gt=`
  mutation resumeDownload($messageId: ID!, $peerId: ID!) {
    resumeDownload(messageId: $messageId, peerId: $peerId)
  }
`,_t=`
  mutation retryDownload($messageId: ID!, $peerId: ID!) {
    retryDownload(messageId: $messageId, peerId: $peerId)
  }
`;export{W as $,ve as $t,ct as A,Se as At,X as B,be as Bt,ce as C,Le as Ct,Be as D,We as Dt,y as E,De as Et,V as F,Ve as Ft,pe as G,Oe as Gt,mt as H,H as Ht,at as I,Ae as It,ke as J,Ke as Jt,de as K,T as Kt,ie as L,ut as Lt,ot as M,Ne as Mt,_e as N,lt as Nt,le as O,Ce as Ot,Qe as P,$ as Pt,P as Q,g as Qt,w as R,He as Rt,qe as S,m as St,v as T,_ as Tt,$e as U,ae as Ut,et as V,ye as Vt,fe as W,Ee as Wt,ze as X,S as Xt,E as Y,Xe as Yt,Re as Z,it as Zt,rt as _,U as _t,K as a,F as at,J as b,b as bt,nt as c,O as ct,ft as d,M as dt,Ie as en,he as et,h as f,B as ft,x as g,pt as gt,Ye as h,k as ht,R as i,Ue as it,se as j,ne as jt,st as k,we as kt,tt as l,Q as lt,N as m,je as mt,D as n,q as nn,dt as nt,Te as o,Je as ot,z as p,Fe as pt,p as q,I as qt,Z as r,j as rn,ht as rt,xe as s,me as st,Ge as t,Y as tn,ge as tt,Pe as u,G as ut,A as v,oe as vt,C as w,re as wt,Ze as x,_t as xt,ue as y,gt as yt,L as z,Me as zt};