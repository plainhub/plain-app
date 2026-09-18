import{C as e}from"./reactivity.esm-bundler-B13GM2Wg.js";import{s as t}from"./temp-D1Kgo3Le.js";import{r as n,t as r}from"./gql-client-GGoHGbr_.js";import{At as i,Dt as a,Et as o,Ot as s,St as c,Tt as l,bt as u,jt as d,wt as f,xt as p,yt as m}from"./query-Detar1oC.js";function ee(i,a=!0){let o=e(!1),s=[],c=[];async function l(e){o.value=!0;try{let o=await n(i.document,e,{dedupe:!1});if(o.errors?.length){let e=o.errors[0].message;a&&t.emit(`toast`,e);let n=new r(e);for(let e of c)e(n);return}for(let e of s)e(o);return o}catch(e){let n=e instanceof r?e.message:`network_error`;a&&t.emit(`toast`,n);for(let t of c)t(e);return}finally{o.value=!1}}function u(e){return s.push(e),{off:()=>{let t=s.indexOf(e);t>=0&&s.splice(t,1)}}}function d(e){return c.push(e),{off:()=>{let t=c.indexOf(e);t>=0&&c.splice(t,1)}}}return{mutate:l,loading:o,onDone:u,onError:d}}async function te(e,t){return await e(t)!=null}var h=`
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
  ${c}
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
  ${c}
`,x=`
  mutation createChatChannel($name: String!) {
    createChatChannel(name: $name) {
      ...ChatChannelFragment
    }
  }
  ${p}
`,S=`
  mutation updateChatChannel($id: ID!, $name: String!) {
    updateChatChannel(id: $id, name: $name) {
      ...ChatChannelFragment
    }
  }
  ${p}
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
  ${p}
`,O=`
  mutation removeChatChannelMember($id: ID!, $peerId: String!) {
    removeChatChannelMember(id: $id, peerId: $peerId) {
      ...ChatChannelFragment
    }
  }
  ${p}
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
  ${a}
`,j=`
  mutation writeTextFile($path: String!, $content: String!, $overwrite: Boolean!) {
    writeTextFile(path: $path, content: $content, overwrite: $overwrite) {
      ...FileFragment
    }
  }
  ${a}
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
  ${i}
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
  mutation trashSms($query: String!) {
    trashSms(query: $query)
  }
`,K=`
  mutation restoreSms($query: String!) {
    restoreSms(query: $query)
  }
`,q=`
  mutation deleteSms($query: String!) {
    deleteSms(query: $query)
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
  mutation saveNote($id: ID!, $input: NoteInput!) {
    saveNote(id: $id, input: $input) {
      ...NoteFragment
    }
  }
  ${s}
`,se=`
  mutation deleteNotes($query: String!) {
    deleteNotes(query: $query)
  }
`,ce=`
  mutation trashNotes($query: String!) {
    trashNotes(query: $query)
  }
`,le=`
  mutation restoreNotes($query: String!) {
    restoreNotes(query: $query)
  }
`,ue=`
  mutation deleteFeedEntries($query: String!) {
    deleteFeedEntries(query: $query)
  }
`,de=`
  mutation deleteCalls($query: String!) {
    deleteCalls(query: $query)
  }
`,fe=`
  mutation deleteContacts($query: String!) {
    deleteContacts(query: $query)
  }
`,pe=`
  mutation createFeed($url: String!, $fetchContent: Boolean!) {
    createFeed(url: $url, fetchContent: $fetchContent) {
      ...FeedFragment
    }
  }
  ${o}
`,me=`
  mutation importFeeds($content: String!) {
    importFeeds(content: $content)
  }
`,he=`
  mutation exportFeeds {
    exportFeeds
  }
`,ge=`
  mutation exportNotes($query: String!) {
    exportNotes(query: $query)
  }
`,_e=`
  mutation relaunchApp {
    relaunchApp
  }
`,ve=`
  mutation openAccessibilitySettings {
    openAccessibilitySettings
  }
`,ye=`
  mutation openWebSettings($feature: AccessFeatureType) {
    openWebSettings(feature: $feature)
  }
`,be=`
  mutation deleteFeed($id: ID!) {
    deleteFeed(id: $id)
  }
`,xe=`
  mutation updateFeed($id: ID!, $name: String!, $fetchContent: Boolean!) {
    updateFeed(id: $id, name: $name, fetchContent: $fetchContent) {
      ...FeedFragment
    }
  }
  ${o}
`,Se=`
  mutation syncFeeds($id: ID) {
    syncFeeds(id: $id)
  }
`,Ce=`
  mutation syncFeedContent($id: ID!) {
    syncFeedContent(id: $id) {
      ...FeedEntryFragment
      feed {
        ...FeedFragment
      }
    }
  }
  ${o}
  ${l}
`,we=`
  mutation call($number: String!, $showDialer: Boolean!) {
    call(number: $number, showDialer: $showDialer)
  }
`,Te=`
  mutation setClip($text: String!) {
    setClip(text: $text)
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
  mutation archiveConversation($id: String!, $date: Long!) {
    archiveConversation(id: $id, date: $date)
  }
`,ke=`
  mutation unarchiveConversation($id: String!) {
    unarchiveConversation(id: $id)
  }
`,Ae=`
  mutation sendMms($number: String!, $body: String!, $attachmentPaths: [String!]!, $threadId: String!) {
    sendMms(number: $number, body: $body, attachmentPaths: $attachmentPaths, threadId: $threadId)
  }
`,je=`
  mutation uninstallPackages($id: ID!) {
    uninstallPackages(ids: [$id])
  }
`,Me=`
  mutation installPackage($path: String!) {
    installPackage(path: $path) {
      packageName
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
  mutation cancelNotifications($ids: [ID!]!) {
    cancelNotifications(ids: $ids)
  }
`,Re=`
  mutation replyNotification($id: ID!, $actionIndex: Int!, $text: String!) {
    replyNotification(id: $id, actionIndex: $actionIndex, text: $text)
  }
`,ze=`
  mutation cancelClipboard($ids: [ID!]!) {
    cancelClipboard(ids: $ids)
  }
`,Be=`
  mutation updateScreenMirrorQuality($mode: ScreenMirrorMode!) {
    updateScreenMirrorQuality(mode: $mode)
  }
`,Ve=`
  mutation saveFeedEntriesToNotes($query: String!) {
    saveFeedEntriesToNotes(query: $query)
  }
`,He=`
  mutation mergeChunks($fileId: String!, $totalChunks: Int!, $path: String!, $replace: Boolean!, $isAppFile: Boolean!, $totalSize: Long!) {
    mergeChunks(fileId: $fileId, totalChunks: $totalChunks, path: $path, replace: $replace, isAppFile: $isAppFile, totalSize: $totalSize)
  }
`,Ue=`
  mutation mergeChunksAsync($fileId: String!, $totalChunks: Int!, $path: String!, $replace: Boolean!, $isAppFile: Boolean!, $totalSize: Long!) {
    mergeChunksAsync(fileId: $fileId, totalChunks: $totalChunks, path: $path, replace: $replace, isAppFile: $isAppFile, totalSize: $totalSize)
  }
`,We=`
  mutation deleteChunks($fileId: String!) {
    deleteChunks(fileId: $fileId)
  }
`,Ge=`
  mutation startPomodoro($timeLeft: Int!) {
    startPomodoro(timeLeft: $timeLeft)
  }
`,Ke=`
  mutation stopPomodoro {
    stopPomodoro
  }
`,qe=`
  mutation pausePomodoro {
    pausePomodoro
  }
`,Je=`
  mutation sendScreenMirrorControl($input: ScreenMirrorControlInput!) {
    sendScreenMirrorControl(input: $input)
  }
`,Ye=`
  mutation addBookmarks($urls: [String!]!, $groupId: String!) {
    addBookmarks(urls: $urls, groupId: $groupId) {
      ...BookmarkFragment
    }
  }
  ${m}
`,Xe=`
  mutation updateBookmark($id: ID!, $input: BookmarkInput!) {
    updateBookmark(id: $id, input: $input) {
      ...BookmarkFragment
    }
  }
  ${m}
`,Ze=`
  mutation deleteBookmarks($ids: [ID!]!) {
    deleteBookmarks(ids: $ids)
  }
`,Qe=`
  mutation recordBookmarkClick($id: ID!) {
    recordBookmarkClick(id: $id)
  }
`,$e=`
  mutation createBookmarkGroup($name: String!) {
    createBookmarkGroup(name: $name) {
      ...BookmarkGroupFragment
    }
  }
  ${u}
`,et=`
  mutation updateBookmarkGroup($id: ID!, $name: String!, $collapsed: Boolean!, $sortOrder: Int!) {
    updateBookmarkGroup(id: $id, name: $name, collapsed: $collapsed, sortOrder: $sortOrder) {
      ...BookmarkGroupFragment
    }
  }
  ${u}
`,tt=`
  mutation deleteBookmarkGroup($id: ID!) {
    deleteBookmarkGroup(id: $id)
  }
`,nt=`
  mutation deleteFiles($paths: [String!]!) {
    deleteFiles(paths: $paths)
  }
`,rt=`
  mutation { enableImageSearch }
`,it=`
  mutation { disableImageSearch }
`,at=`
  mutation { cancelImageModelDownload }
`,ot=`
  mutation startImageIndex($force: Boolean) {
    startImageIndex(force: $force)
  }
`,st=`
  mutation { cancelImageIndex }
`,ct=`
  mutation createContact($input: ContactInput!) {
    createContact(input: $input) {
      ...ContactFragment
    }
  }
  ${f}
`,lt=`
  mutation updateContact($id: ID!, $input: ContactInput!) {
    updateContact(id: $id, input: $input) {
      ...ContactFragment
    }
  }
  ${f}
`,ut=`
  mutation DeleteNote($query: String!) {
    deleteNotes(query: $query)
  }
`,dt=`
  mutation deleteFeedEntry($query: String!) {
    deleteFeedEntries(query: $query)
  }
`,ft=`
  mutation DeleteDataStoreEntry($key: String!) {
    deleteDataStoreEntry(key: $key)
  }
`,$=`
  mutation DeleteDbTableRows($table: String!, $ids: [String!]!) {
    deleteDbTableRows(table: $table, ids: $ids)
  }
`,pt=`
  mutation {
    startDiscovery
  }
`,mt=`
  mutation {
    stopDiscovery
  }
`,ht=`
  mutation pairDevice($input: PairingDeviceInput!) {
    pairDevice(input: $input)
  }
`,gt=`
  mutation cancelPairing($deviceId: String!) {
    cancelPairing(deviceId: $deviceId)
  }
`,_t=`
  mutation respondToPairing($input: PairingRequestInput!, $accepted: Boolean!) {
    respondToPairing(input: $input, accepted: $accepted)
  }
`,vt=`
  mutation downloadPeerFile($messageId: ID!, $peerId: ID!) {
    downloadPeerFile(messageId: $messageId, peerId: $peerId)
  }
`,yt=`
  mutation pauseDownload($messageId: ID!) {
    pauseDownload(messageId: $messageId)
  }
`,bt=`
  mutation resumeDownload($messageId: ID!, $peerId: ID!) {
    resumeDownload(messageId: $messageId, peerId: $peerId)
  }
`,xt=`
  mutation retryDownload($messageId: ID!, $peerId: ID!) {
    retryDownload(messageId: $messageId, peerId: $peerId)
  }
`;export{He as $,et as $t,ft as A,Je as At,L as B,mt as Bt,Ze as C,b as Ct,y as D,oe as Dt,v as E,Ve as Et,nt as F,Ie as Ft,rt as G,H as Gt,ne as H,Fe as Ht,V as I,pt as It,me as J,ke as Jt,he as K,ce as Kt,ut as L,ot as Lt,ue as M,De as Mt,dt as N,Te as Nt,We as O,_ as Ot,be as P,ae as Pt,Ue as Q,Xe as Qt,se as R,Ge as Rt,tt as S,bt as St,C as T,te as Tt,it as U,Ce as Ut,q as V,Ke as Vt,vt as W,Se as Wt,Me as X,T as Xt,ee as Y,je as Yt,E as Z,I as Zt,x as _,k as _t,Y as a,Q as an,yt as at,pe as b,le as bt,ze as c,Qe as ct,Le as d,ie as dt,S as en,P as et,gt as f,J as ft,$e as g,Pe as gt,N as h,Re as ht,R as i,Be as in,ht as it,$ as j,Ee as jt,fe as k,Ae as kt,st as l,_e as lt,z as m,B as mt,D as n,g as nn,ve as nt,Oe as o,X as on,qe as ot,h as p,M as pt,ge as q,G as qt,re as r,xe as rn,ye as rt,we as s,j as sn,F as st,Ye as t,lt as tn,W as tt,at as u,O as ut,ct as v,_t as vt,de as w,xt as wt,Z as x,K as xt,A as y,U as yt,w as z,Ne as zt};