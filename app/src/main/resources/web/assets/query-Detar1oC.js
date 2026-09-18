import{C as e}from"./reactivity.esm-bundler-B13GM2Wg.js";import{Ht as t,L as n,nt as r,ot as i,tt as a}from"./runtime-core.esm-bundler-DY-H7G2c.js";import{r as o,t as s}from"./gql-client-GGoHGbr_.js";var c=`
  fragment TagFragment on Tag {
    id
    name
    count
  }
`,l=`
  fragment TagSubFragment on Tag {
    id
    name
  }
`,u=`
  fragment PlaylistAudioFragment on PlaylistAudio {
    title
    artist
    path
    duration
  }
`,d=`
  fragment AppFragment on App {
    clientId
    urlToken
    httpPort
    httpsPort
    appDir
    deviceName
    deviceType
    battery
    appVersion
    osVersion
    channel
    permissions
    audioCurrent
    audioMode
    sdcardPath
    usbDiskPaths
    internalStoragePath
    downloadsDir
    developerMode
    debug
  }
`,f=`
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
`,p=`
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
  ${l}
`,m=`
  fragment MessageConversationFragment on MessageConversation {
    id
    address
    snippet
    date
    messageCount
    read
  }
`,h=`
  fragment MessageConversationWithAddressesFragment on MessageConversation {
    id
    address
    addresses
    snippet
    date
    messageCount
    read
  }
`,g=`
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
  ${l}
  fragment ContentItemFagment on ContentItem {
    label
    value
    type
  }
`,ee=`
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
  ${l}
`,_=`
  fragment FileFragment on File {
    path
    isDir
    createdAt
    updatedAt
    size
    children
    mediaId
  }
`,v=`
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
  ${l}
`,y=`
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
  ${l}
`,b=`
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
  ${l}
`,x=`
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
  ${l}
`,S=`
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
  ${l}
`,C=`
  fragment FeedFragment on Feed {
    id
    name
    url
    fetchContent
    createdAt
    updatedAt
  }
`,w=`
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
  ${l}
`,T=`
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
`,E=`
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
`,D=`
  fragment ClipboardFragment on Clipboard {
    id
    text
    source
    label
    sensitive
    createdAt
  }
`,te=`
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
`,O=`
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
`,k=`
  fragment BookmarkGroupFragment on BookmarkGroup {
    id
    name
    collapsed
    sortOrder
    createdAt
    updatedAt
  }
`,A=`
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

`;function j(e){return e instanceof s?e.status===403?`desktop_access_disabled`:e.message:`network_error`}function M(e){if(e)return typeof e==`function`?e():e}function N(s){let c=e(!1),l=e();async function u(e){c.value=!0;try{let t=e??M(s.variables),n=await o(s.document,t);n.errors?.length?s.handle(n.data,n.errors[0].message):(l.value=n.data,s.handle(n.data,``))}catch(e){s.handle(void 0,j(e))}finally{c.value=!1}}if(u(),typeof s.variables==`function`){let e=!0;n()&&(i(()=>{e=!1}),r(()=>{e=!0})),t(s.variables,async()=>{await a(),e&&u()},{deep:!0})}return{loading:c,result:l,refetch:u}}function P(t){let n=e(!1),r=e(),i=0,a=0,s=0,c;async function l(e,l={}){let u=++i,d=l.latest?++a:void 0,f=e??M(t.variables),p=f?{...f}:void 0,m={variables:p,requestId:u,meta:l.meta};l.latest?c=u:s++,n.value=!0;try{let e=l.force?await o(t.document,p,{fresh:!0}):await o(t.document,p);if(l.latest&&d!==a)return;e.errors?.length?t.handle(e.data,e.errors[0].message,m):(r.value=e.data,t.handle(e.data,``,m))}catch(e){(!l.latest||d===a)&&t.handle(void 0,j(e),m)}finally{l.latest?c===u&&(c=void 0):s--,n.value=s>0||c!==void 0}}return{loading:n,result:r,fetch:l}}var F=`
  query ($id: String!) {
    chatItems(id: $id) {
      ...ChatItemFragment
    }
  }
  ${f}
`,I=`
  query ($id: String!) {
    chatItem(id: $id) {
      ...ChatItemFragment
    }
  }
  ${f}
`,L=`
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
`,R=`
  query {
    latestChatItems {
      ...ChatItemFragment
    }
  }
  ${f}
`,z=`
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
`,B=`
  query {
    chatChannels {
      ...ChatChannelFragment
    }
  }
  ${A}
`,V=`
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
  ${l}
`,H=`
  query sms($offset: Int!, $limit: Int!, $query: String!) {
    sms(offset: $offset, limit: $limit, query: $query) {
      ...MessageFragment
    }
    smsCount(query: $query)
  }
  ${p}
`,U=`
  query {
    sims {
      id
      label
      number
      subscriptionId
    }
  }
`,W=`
  query smsConversations($offset: Int!, $limit: Int!, $query: String!) {
    smsConversations(offset: $offset, limit: $limit, query: $query) {
      ...MessageConversationFragment
    }
    smsConversationCount(query: $query)
  }
  ${m}
`,G=`
  query smsConversations($offset: Int!, $limit: Int!, $query: String!) {
    smsConversations(offset: $offset, limit: $limit, query: $query) {
      ...MessageConversationWithAddressesFragment
    }
    smsConversationCount(query: $query)
  }
  ${h}
`,K=`
  query contacts($offset: Int!, $limit: Int!, $query: String!) {
    contacts(offset: $offset, limit: $limit, query: $query) {
      ...ContactFragment
    }
    contactCount(query: $query)
  }
  ${g}
`,q=`
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
`,J=`
  query {
    contactSources {
      name
      type
    }
  }
`,Y=`
  query calls($offset: Int!, $limit: Int!, $query: String!) {
    calls(offset: $offset, limit: $limit, query: $query) {
      ...CallFragment
    }
    callCount(query: $query)
  }
  ${ee}
`,X=`
  query images($offset: Int!, $limit: Int!, $query: String!, $sortBy: FileSortBy!) {
    images(offset: $offset, limit: $limit, query: $query, sortBy: $sortBy) {
      ...ImageFragment
    }
    imageCount(query: $query)
  }
  ${v}
`,Z=`
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
`,Q=`
  query videos($offset: Int!, $limit: Int!, $query: String!, $sortBy: FileSortBy!) {
    videos(offset: $offset, limit: $limit, query: $query, sortBy: $sortBy) {
      ...VideoFragment
    }
    videoCount(query: $query)
  }
  ${y}
`,ne=`
  query audios($offset: Int!, $limit: Int!, $query: String!, $sortBy: FileSortBy!) {
    items: audios(offset: $offset, limit: $limit, query: $query, sortBy: $sortBy) {
      ...AudioFragment
    }
    total: audioCount(query: $query)
  }
  ${b}
`,re=`
  query files($root: String!, $offset: Int!, $limit: Int!, $query: String!, $sortBy: FileSortBy!) {
    files(root: $root, offset: $offset, limit: $limit, query: $query, sortBy: $sortBy) {
      ...FileFragment
    }
  }
  ${_}
`,ie=`
  query recentFiles {
    recentFiles {
      ...FileFragment
    }
  }
  ${_}
`,ae=`
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
`,oe=`
  query {
    favoriteFolders {
      rootPath
      fullPath
      alias
    }
  }
`,se=`
  query {
    app {
      ...AppFragment
    }
  }
  ${d}
`,ce=`
  query audioPlaylist($offset: Int!, $limit: Int!) {
    audioPlaylist(offset: $offset, limit: $limit) {
      total
      items {
        ...PlaylistAudioFragment
      }
    }
  }
  ${u}
`,le=`
  query tags($type: DataType!) {
    tags(type: $type) {
      ...TagFragment
    }
  }
  ${c}
`,ue=`
  query mediaBuckets($type: DataType!) {
    mediaBuckets(type: $type) {
      id
      name
      itemCount
      topItems
    }
  }
`,de=`
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
  ${l}
`,fe=`
  query note($id: ID!) {
    note(id: $id) {
      ...NoteFragment
    }
  }
  ${x}
`,pe=`
  query {
    feeds {
      ...FeedFragment
    }
  }
  ${C}
`,me=`
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
  ${l}
`,he=`
  query feedsTags($type: DataType!) {
    tags(type: $type) {
      ...TagFragment
    }
    feeds {
      ...FeedFragment
    }
  }
  ${C}
  ${c}
`,ge=`
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
  ${c}
`,_e=`
  query feedEntry($id: ID!) {
    feedEntry(id: $id) {
      ...FeedEntryFragment
      feed {
        ...FeedFragment
      }
    }
  }
  ${C}
  ${w}
`,ve=`
  query imageCount($query: String!) {
    total: imageCount(query: $query)
    trash: imageCount(query: "trash:true")
  }
`,ye=`
  query audioCount($query: String!) {
    total: audioCount(query: $query)
    trash: audioCount(query: "trash:true")
  }
`,be=`
  query docs($offset: Int!, $limit: Int!, $query: String!, $sortBy: FileSortBy!) {
    items: docs(offset: $offset, limit: $limit, query: $query, sortBy: $sortBy) {
      ...DocFragment
    }
    total: docCount(query: $query)
  }
  ${S}
`,xe=`
  query docCount($query: String!) {
    total: docCount(query: $query)
    trash: docCount(query: "trash:true")
    extGroups: docExtGroups {
      ext
      count
    }
  }
`,Se=`
  query videoCount($query: String!) {
    total: videoCount(query: $query)
    trash: videoCount(query: "trash:true")
  }
`,Ce=`
  query {
    total: packageCount(query: "")
    system: packageCount(query: "type:SYSTEM")
  }
`,we=`
  query {
    total: feedEntryCount(query: "")
    today: feedEntryCount(query: "today:true")
    feedsCount {
      id
      count
    }
  }
`,Te=`
  query {
    total: contactCount(query: "")
  }
`,Ee=`
  query {
    total: callCount(query: "")
    incoming: callCount(query: "type:1")
    outgoing: callCount(query: "type:2")
    missed: callCount(query: "type:3")
  }
`,De=`
  query {
    smsAllCounts {
      total
      inbox
      sent
      drafts
    }
  }
`,Oe=`
  query {
    archivedConversations {
      ...MessageConversationFragment
    }
  }
  ${m}
`,ke=`
  query {
    archivedConversations {
      ...MessageConversationWithAddressesFragment
    }
  }
  ${h}
`,Ae=`
  query {
    total: noteCount(query: "")
    trash: noteCount(query: "trash:true")
  }
`,je=`
  query packages($offset: Int!, $limit: Int!, $query: String!, $sortBy: FileSortBy!) {
    packages(offset: $offset, limit: $limit, query: $query, sortBy: $sortBy) {
      ...PackageFragment
    }
    packageCount(query: $query)
  }
  ${T}
`,Me=`
  query packageStatuses($ids: [ID!]!) {
    packageStatuses(ids: $ids) {
      id
      exist
      updatedAt
    }
  }
`,Ne=`
  query {
    screenMirrorState
    screenMirrorControlEnabled
    screenMirrorQuality {
      mode
      resolution
    }
  }
`,Pe=`
  query {
    screenMirrorVideoCodec {
      annexB
      keyFrame
    }
  }
`,Fe=`
  query {
    screenMirrorControlEnabled
  }
`,Ie=`
  mutation {
    requestScreenMirrorKeyFrame
  }
`,Le=`
  query {
    notifications {
      ...NotificationFragment
    }
  }
  ${E}
`,Re=`
  query clipboard($offset: Int!, $limit: Int!, $query: String!) {
    clipboard(offset: $offset, limit: $limit, query: $query) {
      ...ClipboardFragment
    }
    clipboardCount(query: $query)
  }
  ${D}
`,ze=`
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
  ${te}
`,Be=`
  query AppLogs($offset: Int!, $limit: Int!) {
    appLogs(offset: $offset, limit: $limit)
  }
`,Ve=`
  query {
    appLogPath
  }
`,$=`
  query {
    dbPath
  }
`,He=`
  query {
    dataStorePath
  }
`,Ue=`
  query uploadedChunks($fileId: String!) {
    uploadedChunks(fileId: $fileId)
  }
`,We=`
  query mergeStatus($fileId: String!) {
    mergeStatus(fileId: $fileId)
  }
`,Ge=`
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
`,Ke=`
  query {
    dataStoreEntries {
      key
      value
    }
  }
`,qe=`
  query {
    dbTables
  }
`,Je=`
  query DbTableRowCount($table: String!) {
    dbTableRowCount(table: $table)
  }
`,Ye=`
  query DbTableRows($table: String!, $offset: Int!, $limit: Int!) {
    dbTableRows(table: $table, offset: $offset, limit: $limit)
  }
`,Xe=`
  query DbTableInfo($table: String!) {
    dbTableInfo(table: $table) {
      idKey
    }
  }
`,Ze=`
  query {
    bookmarks {
      ...BookmarkFragment
    }
    bookmarkGroups {
      ...BookmarkGroupFragment
    }
  }
  ${O}
  ${k}
`,Qe=`
  query {
    isDiscovering
  }
`;export{Le as $,be as A,u as At,ve as B,$ as C,D as Ct,qe as D,_ as Dt,Ye as E,C as Et,pe as F,Qe as G,X as H,he as I,We as J,R as K,V as L,me as M,we as N,ze as O,x as Ot,_e as P,de as Q,re as R,He as S,f as St,Je as T,w as Tt,P as U,Z as V,N as W,Ae as X,ae as Y,fe as Z,Re as _,Se as _t,Oe as a,ie as at,K as b,k as bt,ce as c,Ne as ct,ge as d,W as dt,Ce as et,Ee as f,G as ft,F as g,Ue as gt,I as h,le as ht,Be as i,Ge as it,oe as j,c as jt,xe as k,E as kt,ne as l,Pe as lt,B as m,H as mt,se as n,je as nt,ke as o,Ie as ot,Y as p,De as pt,ue as q,Ve as r,L as rt,ye as s,Fe as st,z as t,Me as tt,Ze as u,U as ut,Te as v,Q as vt,Xe as w,g as wt,Ke as x,A as xt,J as y,O as yt,q as z};