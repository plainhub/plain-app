import{Ht as e,L as t,nt as n,ot as r,tt as i,vn as a}from"./runtime-core.esm-bundler-DrnlzXx3.js";import{r as o,t as s}from"./gql-client-Da0Gzuip.js";var c=`
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
    usbConnected
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
  ${u}
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
`,_=`
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
`,v=`
  fragment FileFragment on File {
    path
    isDir
    createdAt
    updatedAt
    size
    children
    mediaId
  }
`,y=`
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
`,b=`
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
`,ee=`
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
`,D=`
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
`,O=`
  fragment BookmarkGroupFragment on BookmarkGroup {
    id
    name
    collapsed
    sortOrder
    createdAt
    updatedAt
  }
`,k=`
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

`;function A(e){return e instanceof s?e.status===403?`desktop_access_disabled`:e.message:`network_error`}function j(e){if(e)return typeof e==`function`?e():e}function M(s){let c=a(!1),l=a();async function u(e){c.value=!0;try{let t=e??j(s.variables),n=await o(s.document,t);n.errors?.length?s.handle(n.data,n.errors[0].message):(l.value=n.data,s.handle(n.data,``))}catch(e){s.handle(void 0,A(e))}finally{c.value=!1}}if(u(),typeof s.variables==`function`){let a=!0;t()&&(r(()=>{a=!1}),n(()=>{a=!0})),e(s.variables,async()=>{await i(),a&&u()},{deep:!0})}return{loading:c,result:l,refetch:u}}function N(e){let t=a(!1),n=a(),r=0,i=0,s=0,c;async function l(a,l={}){let u=++r,d=l.latest?++i:void 0,f=a??j(e.variables),p=f?{...f}:void 0,m={variables:p,requestId:u,meta:l.meta};l.latest?c=u:s++,t.value=!0;try{let t=l.force?await o(e.document,p,{fresh:!0}):await o(e.document,p);if(l.latest&&d!==i)return;t.errors?.length?e.handle(t.data,t.errors[0].message,m):(n.value=t.data,e.handle(t.data,``,m))}catch(t){(!l.latest||d===i)&&e.handle(void 0,A(t),m)}finally{l.latest?c===u&&(c=void 0):s--,t.value=s>0||c!==void 0}}return{loading:t,result:n,fetch:l}}var P=`
  query ($id: String!) {
    chatItems(id: $id) {
      ...ChatItemFragment
    }
  }
  ${f}
`,F=`
  query ($id: String!) {
    chatItem(id: $id) {
      ...ChatItemFragment
    }
  }
  ${f}
`,I=`
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
`,L=`
  query {
    latestChatItems {
      ...ChatItemFragment
    }
  }
  ${f}
`,R=`
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
`,z=`
  query {
    chatChannels {
      ...ChatChannelFragment
    }
  }
  ${k}
`,B=`
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
`,V=`
  query sms($offset: Int!, $limit: Int!, $query: String!) {
    sms(offset: $offset, limit: $limit, query: $query) {
      ...MessageFragment
    }
    smsCount(query: $query)
  }
  ${p}
`,H=`
  query {
    sims {
      id
      label
      number
      subscriptionId
    }
  }
`,U=`
  query smsConversations($offset: Int!, $limit: Int!, $query: String!) {
    smsConversations(offset: $offset, limit: $limit, query: $query) {
      ...MessageConversationFragment
    }
    smsConversationCount(query: $query)
  }
  ${m}
`,W=`
  query smsConversations($offset: Int!, $limit: Int!, $query: String!) {
    smsConversations(offset: $offset, limit: $limit, query: $query) {
      ...MessageConversationWithAddressesFragment
    }
    smsConversationCount(query: $query)
  }
  ${h}
`,G=`
  query contacts($offset: Int!, $limit: Int!, $query: String!) {
    contacts(offset: $offset, limit: $limit, query: $query) {
      ...ContactFragment
    }
    contactCount(query: $query)
  }
  ${g}
`,K=`
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
`,q=`
  query {
    contactSources {
      name
      type
    }
  }
`,J=`
  query calls($offset: Int!, $limit: Int!, $query: String!) {
    calls(offset: $offset, limit: $limit, query: $query) {
      ...CallFragment
    }
    callCount(query: $query)
  }
  ${_}
`,Y=`
  query images($offset: Int!, $limit: Int!, $query: String!, $sortBy: FileSortBy!) {
    images(offset: $offset, limit: $limit, query: $query, sortBy: $sortBy) {
      ...ImageFragment
    }
    imageCount(query: $query)
  }
  ${y}
`,X=`
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
`,Z=`
  query videos($offset: Int!, $limit: Int!, $query: String!, $sortBy: FileSortBy!) {
    videos(offset: $offset, limit: $limit, query: $query, sortBy: $sortBy) {
      ...VideoFragment
    }
    videoCount(query: $query)
  }
  ${b}
`,Q=`
  query audios($offset: Int!, $limit: Int!, $query: String!, $sortBy: FileSortBy!) {
    items: audios(offset: $offset, limit: $limit, query: $query, sortBy: $sortBy) {
      ...AudioFragment
    }
    total: audioCount(query: $query)
  }
  ${ee}
`,ne=`
  query files($root: String!, $offset: Int!, $limit: Int!, $query: String!, $sortBy: FileSortBy!) {
    files(root: $root, offset: $offset, limit: $limit, query: $query, sortBy: $sortBy) {
      ...FileFragment
    }
  }
  ${v}
`,re=`
  query recentFiles {
    recentFiles {
      ...FileFragment
    }
  }
  ${v}
`,ie=`
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
`,ae=`
  query {
    app {
      ...AppFragment
    }
  }
  ${d}
`,oe=`
  query tags($type: DataType!) {
    tags(type: $type) {
      ...TagFragment
    }
  }
  ${c}
`,se=`
  query mediaBuckets($type: DataType!) {
    mediaBuckets(type: $type) {
      id
      name
      itemCount
      topItems
    }
  }
`,ce=`
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
`,le=`
  query note($id: ID!) {
    note(id: $id) {
      ...NoteFragment
    }
  }
  ${x}
`,ue=`
  query {
    feeds {
      ...FeedFragment
    }
  }
  ${C}
`,de=`
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
`,fe=`
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
`,pe=`
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
`,me=`
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
`,he=`
  query imageCount($query: String!) {
    total: imageCount(query: $query)
    trash: imageCount(query: "trash:true")
  }
`,ge=`
  query audioCount($query: String!) {
    total: audioCount(query: $query)
    trash: audioCount(query: "trash:true")
  }
`,_e=`
  query docs($offset: Int!, $limit: Int!, $query: String!, $sortBy: FileSortBy!) {
    items: docs(offset: $offset, limit: $limit, query: $query, sortBy: $sortBy) {
      ...DocFragment
    }
    total: docCount(query: $query)
  }
  ${S}
`,ve=`
  query docCount($query: String!) {
    total: docCount(query: $query)
    trash: docCount(query: "trash:true")
    extGroups: docExtGroups {
      ext
      count
    }
  }
`,ye=`
  query videoCount($query: String!) {
    total: videoCount(query: $query)
    trash: videoCount(query: "trash:true")
  }
`,be=`
  query {
    total: packageCount(query: "")
    system: packageCount(query: "type:SYSTEM")
  }
`,xe=`
  query {
    total: feedEntryCount(query: "")
    today: feedEntryCount(query: "today:true")
    feedsCount {
      id
      count
    }
  }
`,Se=`
  query {
    total: contactCount(query: "")
  }
`,Ce=`
  query {
    total: callCount(query: "")
    incoming: callCount(query: "type:1")
    outgoing: callCount(query: "type:2")
    missed: callCount(query: "type:3")
  }
`,we=`
  query {
    smsAllCounts {
      total
      inbox
      sent
      drafts
    }
  }
`,Te=`
  query {
    archivedConversations {
      ...MessageConversationFragment
    }
  }
  ${m}
`,Ee=`
  query {
    archivedConversations {
      ...MessageConversationWithAddressesFragment
    }
  }
  ${h}
`,De=`
  query {
    total: noteCount(query: "")
    trash: noteCount(query: "trash:true")
  }
`,Oe=`
  query packages($offset: Int!, $limit: Int!, $query: String!, $sortBy: FileSortBy!) {
    packages(offset: $offset, limit: $limit, query: $query, sortBy: $sortBy) {
      ...PackageFragment
    }
    packageCount(query: $query)
  }
  ${T}
`,ke=`
  query packageStatuses($ids: [ID!]!) {
    packageStatuses(ids: $ids) {
      id
      exist
      updatedAt
    }
  }
`,Ae=`
  query {
    screenMirrorState
    screenMirrorControlEnabled
    screenMirrorQuality {
      mode
      resolution
    }
  }
`,je=`
  query {
    screenMirrorVideoCodec {
      annexB
      keyFrame
    }
  }
`,Me=`
  query {
    screenMirrorControlEnabled
  }
`,Ne=`
  mutation {
    requestScreenMirrorKeyFrame
  }
`,Pe=`
  query {
    notifications {
      ...NotificationFragment
    }
  }
  ${E}
`,$=`
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
`,Fe=`
  query AppLogs($offset: Int!, $limit: Int!) {
    appLogs(offset: $offset, limit: $limit)
  }
`,Ie=`
  query {
    appLogPath
  }
`,Le=`
  query {
    dbPath
  }
`,Re=`
  query {
    dataStorePath
  }
`,ze=`
  query uploadedChunks($fileId: String!) {
    uploadedChunks(fileId: $fileId)
  }
`,Be=`
  query mergeStatus($fileId: String!) {
    mergeStatus(fileId: $fileId)
  }
`,Ve=`
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
`,He=`
  query {
    dataStoreEntries {
      key
      value
    }
  }
`,Ue=`
  query {
    dbTables
  }
`,We=`
  query DbTableRowCount($table: String!) {
    dbTableRowCount(table: $table)
  }
`,Ge=`
  query DbTableRows($table: String!, $offset: Int!, $limit: Int!) {
    dbTableRows(table: $table, offset: $offset, limit: $limit)
  }
`,Ke=`
  query DbTableInfo($table: String!) {
    dbTableInfo(table: $table) {
      idKey
    }
  }
`,qe=`
  query {
    bookmarks {
      ...BookmarkFragment
    }
    bookmarkGroups {
      ...BookmarkGroupFragment
    }
  }
  ${D}
  ${O}
`,Je=`
  query {
    isDiscovering
  }
`;export{Oe as $,xe as A,N as B,We as C,v as Ct,ve as D,c as Dt,$ as E,u as Et,ne as F,Be as G,Je as H,K as I,le as J,ie as K,he as L,ue as M,fe as N,_e as O,B as P,ke as Q,X as R,Ke as S,C as St,Ue as T,E as Tt,L as U,M as V,se as W,Pe as X,ce as Y,be as Z,q as _,O as _t,Te as a,Ae as at,Re as b,g as bt,Q as c,U as ct,Ce as d,V as dt,I as et,J as f,oe as ft,Se as g,D as gt,P as h,Z as ht,Fe as i,Me as it,me as j,de as k,qe as l,W as lt,F as m,ye as mt,ae as n,re as nt,Ee as o,je as ot,z as p,ze as pt,De as q,Ie as r,Ne as rt,ge as s,H as st,R as t,Ve as tt,pe as u,we as ut,G as v,k as vt,Ge as w,x as wt,Le as x,w as xt,He as y,f as yt,Y as z};