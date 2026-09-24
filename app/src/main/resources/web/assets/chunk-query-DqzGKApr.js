import{Ht as e,L as t,nt as n,ot as r,tt as i,vn as a}from"./chunk-runtime-core.esm-bundler-DrnlzXx3.js";import{r as o,t as s}from"./chunk-gql-client-Y9_mtvUa.js";var c=`
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
  fragment AudioItemFragment on AudioItem {
    title
    artist
    path
    durationMs
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
    capabilities
    buildChannel
    permissions
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
      ... on ChatImages {
        ids
      }
      ... on ChatFiles {
        ids
      }
      ... on ChatText {
        linkPreviewImageIds
      }
    }
  }
`,p=`
  fragment SmsFragment on Sms {
    id
    body
    address
    serviceCenter
    sentAt
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
  fragment SmsConversationFragment on SmsConversation {
    id
    address
    snippet
    lastMessageAt
    messageCount
    read
  }
`,h=`
  fragment SmsConversationWithAddressesFragment on SmsConversation {
    id
    address
    addresses
    snippet
    lastMessageAt
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
      value
      type
      label
      normalizedNumber
    }
    addresses {
      value
      type
      label
    }
    emails {
      value
      type
      label
    }
    websites {
      value
      type
      label
    }
    events {
      value
      type
      label
    }
    ims {
      value
      protocol
      customProtocol
    }
    tags {
      ...TagSubFragment
    }
  }
  ${l}
`,ee=`
  fragment CallFragment on Call {
    id
    name
    number
    durationSec
    accountId
    startedAt
    photoId
    type
    geo {
      country
      numberType
      carrier
      description
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
    childCount
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
    durationMs
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
    durationMs
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
    postedAt
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
    cpuArch
    cpuModel
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
      buildNumber
      device
      javaVmVersion
      glEsVersion
      buildTime
    }
  }
`,O=`
  fragment DeviceStatusFragment on DeviceStatus {
    uptimeSec
    batteryLevel
    charging
    temperatures {
      label
      celsius
    }
    cpuUsage
    memoryAvailable
    storageAvailable
  }
`,k=`
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
`,A=`
  fragment BookmarkGroupFragment on BookmarkGroup {
    id
    name
    collapsed
    sortOrder
    itemCount
    createdAt
    updatedAt
  }
`,j=`
  fragment ChatChannelFragment on ChatChannel {
    id
    name
    ownerId
    members {
      ...ChatChannelMemberFragment
    }
    version
    status
    createdAt
    updatedAt
  }
  
  fragment ChatChannelMemberFragment on ChatChannelMember {
    peerId
    status
  }

`;function M(e){return e instanceof s?e.status===403?`desktop_access_disabled`:e.message:`network_error`}function N(e){if(e)return typeof e==`function`?e():e}function P(s){let c=a(!1),l=a();async function u(e){if(!(s.enabled&&!s.enabled())){c.value=!0;try{let t=e??N(s.variables),n=await o(typeof s.document==`function`?s.document():s.document,t);n.errors?.length?s.handle(n.data,n.errors[0].message):(l.value=n.data,s.handle(n.data,``))}catch(e){s.handle(void 0,M(e))}finally{c.value=!1}}}u();let d=!0;return t()&&(r(()=>{d=!1}),n(()=>{d=!0})),typeof s.variables==`function`&&e(s.variables,async()=>{await i(),d&&u()},{deep:!0}),typeof s.document==`function`&&e(s.document,async()=>{await i(),d&&u()}),s.enabled&&e(s.enabled,async(e,t)=>{await i(),d&&e&&!t&&u()}),{loading:c,result:l,refetch:u}}function F(e){let t=a(!1),n=a(),r=0,i=0,s=0,c;async function l(a,l={}){let u=++r,d=l.latest?++i:void 0,f=a??N(e.variables),p=f?{...f}:void 0,m={variables:p,requestId:u,meta:l.meta};l.latest?c=u:s++,t.value=!0;try{let t=typeof e.document==`function`?e.document():e.document,r=l.force?await o(t,p,{fresh:!0}):await o(t,p);if(l.latest&&d!==i)return;r.errors?.length?e.handle(r.data,r.errors[0].message,m):(n.value=r.data,e.handle(r.data,``,m))}catch(t){(!l.latest||d===i)&&e.handle(void 0,M(t),m)}finally{l.latest?c===u&&(c=void 0):s--,t.value=s>0||c!==void 0}}return{loading:t,result:n,fetch:l}}var I=`
  query ($target: String!) {
    chatItems(target: $target, offset: 0, limit: 200, query: "") {
      ...ChatItemFragment
    }
  }
  ${f}
`,L=`
  query ($id: String!) {
    chatItem(id: $id) {
      ...ChatItemFragment
    }
  }
  ${f}
`,R=`
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
`,z=`
  query {
    latestChatItems {
      ...ChatItemFragment
    }
  }
  ${f}
`,B=`
  query appFiles($offset: Int!, $limit: Int!) {
    appFiles(offset: $offset, limit: $limit, query: "") {
      id
      size
      mimeType
      fileName
      createdAt
      updatedAt
    }
    appFileCount(query: "")
  }
`,V=`
  query ($type: DataType!, $keys: [String!]!) {
    tagRelations(type: $type, keys: $keys) {
      tagId
      key
    }
  }
`,H=`
  query {
    chatChannels {
      ...ChatChannelFragment
    }
  }
  ${j}
`,U=`
  query ($path: String!, $fileName: String) {
    fileInfo(path: $path, fileName: $fileName) {
      ... on FileInfo {
        path
        updatedAt
        size
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
          durationMs
          width
          height
          location {
            latitude
            longitude
          }
        }
        ... on AudioFileInfo {
          durationMs
          location {
            latitude
            longitude
          }
        }
      }
    }
  }
`,W=`
  query sms($offset: Int!, $limit: Int!, $query: String!) {
    sms(offset: $offset, limit: $limit, query: $query) {
      ...SmsFragment
    }
    smsCount(query: $query)
  }
  ${p}
`,G=`
  query {
    sims {
      id
      label
      number
      subscriptionId
    }
  }
`,K=`
  query smsConversations($offset: Int!, $limit: Int!, $query: String!) {
    smsConversations(offset: $offset, limit: $limit, query: $query) {
      ...SmsConversationFragment
    }
    smsConversationCount(query: $query)
  }
  ${m}
`,q=`
  query smsConversations($offset: Int!, $limit: Int!, $query: String!) {
    smsConversations(offset: $offset, limit: $limit, query: $query) {
      ...SmsConversationWithAddressesFragment
    }
    smsConversationCount(query: $query)
  }
  ${h}
`,J=`
  query contacts($offset: Int!, $limit: Int!, $query: String!) {
    contacts(offset: $offset, limit: $limit, query: $query) {
      ...ContactFragment
    }
    contactCount(query: $query)
  }
  ${g}
`,Y={audios:`audioCount(query: $mediaQuery)`,images:`imageCount(query: $mediaQuery)`,videos:`videoCount(query: $mediaQuery)`,docs:`docCount(query: "")`,packages:`packageCount(query: "")`,notes:`noteCount(query: "")`,feedEntries:`feedEntryCount(query: "")`,messages:`smsCount(query: "")`,calls:`callCount(query: "")`,contacts:`contactCount(query: "")`};function X(e=[]){return`
  query homeStats($mediaQuery: String!) {
    ${e.map(e=>Y[e]).join(`
    `)}
    mounts {
      id
      path
      mountPoint
      totalBytes
      freeBytes
      driveType
    }
  }
`}var Z=`
  query {
    contactSources {
      name
      type
    }
  }
`,Q=`
  query calls($offset: Int!, $limit: Int!, $query: String!) {
    calls(offset: $offset, limit: $limit, query: $query) {
      ...CallFragment
    }
    callCount(query: $query)
  }
  ${ee}
`,ne=`
  query images($offset: Int!, $limit: Int!, $query: String!, $sortBy: FileSortBy!) {
    images(offset: $offset, limit: $limit, query: $query, sortBy: $sortBy) {
      ...ImageFragment
    }
    imageCount(query: $query)
  }
  ${v}
`,re=`
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
`,ie=`
  query {
    scanProgress {
      indexed
      pending
      total
      state
    }
  }
`,ae=`
  query videos($offset: Int!, $limit: Int!, $query: String!, $sortBy: FileSortBy!) {
    videos(offset: $offset, limit: $limit, query: $query, sortBy: $sortBy) {
      ...VideoFragment
    }
    videoCount(query: $query)
  }
  ${y}
`,oe=`
  query audios($offset: Int!, $limit: Int!, $query: String!, $sortBy: FileSortBy!) {
    items: audios(offset: $offset, limit: $limit, query: $query, sortBy: $sortBy) {
      ...AudioFragment
    }
    total: audioCount(query: $query)
  }
  ${b}
`,se=`
  query files($root: String!, $offset: Int!, $limit: Int!, $query: String!, $sortBy: FileSortBy!) {
    files(root: $root, offset: $offset, limit: $limit, query: $query, sortBy: $sortBy) {
      ...FileFragment
    }
  }
  ${_}
`,ce=`
  query recentFiles {
    recentFiles {
      ...FileFragment
    }
  }
  ${_}
`,le=`
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
      diskId
    }
  }
`,ue=`
  query {
    disks {
      id
      name
      path
      sizeBytes
      removable
      model
    }
  }
`,de=`
  query {
    mounts {
      id
      name
      alias
      label
      mountPoint
      fsType
      totalBytes
      usedBytes
      freeBytes
      remote
      driveType
      diskId
      path
      partitionNum
      uuid
    }
  }
`,fe=`
  query {
    sambaSettings {
      enabled
      username
      hasPassword
      shares {
        name
        sharePath
        auth
        readOnly
      }
      serviceName
      serviceActive
      serviceEnabled
    }
  }
`,pe=`
  query {
    favoriteFolders {
      rootPath
      fullPath
      alias
    }
  }
`,me=`
  query {
    app {
      ...AppFragment
    }
  }
  ${d}
`,he=`
  query audioQueue($offset: Int!, $limit: Int!) {
    items: audioQueueItems(offset: $offset, limit: $limit, query: "") {
      ...AudioItemFragment
    }
    total: audioQueueItemCount
    playback: audioPlayback {
      mode
      currentPath
      isPlaying
      positionMs
    }
  }
  ${u}
`,ge=`
  query tags($type: DataType!) {
    tags(type: $type) {
      ...TagFragment
    }
  }
  ${c}
`,_e=`
  query mediaBuckets($type: MediaDataType!) {
    mediaBuckets(type: $type) {
      id
      name
      itemCount
      topItemPaths
    }
  }
`,ve=`
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
`,ye=`
  query note($id: ID!) {
    note(id: $id) {
      ...NoteFragment
    }
  }
  ${x}
`,be=`
  query {
    feeds {
      ...FeedFragment
    }
  }
  ${C}
`,xe=`
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
`,Se=`
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
`,Ce=`
  query bucketsTags($type: MediaDataType!, $tagType: DataType!) {
    tags(type: $tagType) {
      ...TagFragment
    }
    mediaBuckets(type: $type) {
      id
      name
      itemCount
      topItemPaths
    }
  }
  ${c}
`,we=`
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
`,Te=`
  query imageCount($query: String!) {
    total: imageCount(query: $query)
    trash: imageCount(query: "trash:true")
  }
`,Ee=`
  query audioCount($query: String!) {
    total: audioCount(query: $query)
    trash: audioCount(query: "trash:true")
  }
`,De=`
  query docs($offset: Int!, $limit: Int!, $query: String!, $sortBy: FileSortBy!) {
    items: docs(offset: $offset, limit: $limit, query: $query, sortBy: $sortBy) {
      ...DocFragment
    }
    total: docCount(query: $query)
  }
  ${S}
`,Oe=`
  query docCount($query: String!) {
    total: docCount(query: $query)
    trash: docCount(query: "trash:true")
    extGroups: docExtGroups {
      ext
      count
    }
  }
`,ke=`
  query videoCount($query: String!) {
    total: videoCount(query: $query)
    trash: videoCount(query: "trash:true")
  }
`,Ae=`
  query {
    total: packageCount(query: "")
    system: packageCount(query: "type:SYSTEM")
  }
`,je=`
  query {
    total: feedEntryCount(query: "")
    today: feedEntryCount(query: "today:true")
    feedEntryCounts {
      id
      count
    }
  }
`,Me=`
  query {
    total: contactCount(query: "")
  }
`,Ne=`
  query {
    total: callCount(query: "")
    incoming: callCount(query: "type:1")
    outgoing: callCount(query: "type:2")
    missed: callCount(query: "type:3")
  }
`,Pe=`
  query {
    smsBoxCounts {
      total
      inbox
      sent
      drafts
    }
  }
`,Fe=`
  query {
    archivedConversations(offset: 0, limit: 200, query: "") {
      ...SmsConversationFragment
    }
  }
  ${m}
`,Ie=`
  query {
    archivedConversations(offset: 0, limit: 200, query: "") {
      ...SmsConversationWithAddressesFragment
    }
  }
  ${h}
`,Le=`
  query {
    total: noteCount(query: "")
    trash: noteCount(query: "trash:true")
  }
`,Re=`
  query packages($offset: Int!, $limit: Int!, $query: String!, $sortBy: FileSortBy!) {
    packages(offset: $offset, limit: $limit, query: $query, sortBy: $sortBy) {
      ...PackageFragment
    }
    packageCount(query: $query)
  }
  ${T}
`,ze=`
  query packageStatuses($ids: [ID!]!) {
    packageStatuses(ids: $ids) {
      id
      exists
      updatedAt
    }
  }
`,Be=`
  query {
    isScreenMirroring
    screenMirrorControlEnabled
    screenMirrorQuality {
      mode
      resolution
    }
  }
`,Ve=`
  query {
    screenMirrorVideoCodec {
      annexB
      keyFrame
    }
  }
`,He=`
  query {
    screenMirrorControlEnabled
  }
`,Ue=`
  mutation {
    requestScreenMirrorKeyFrame
  }
`,We=`
  query {
    notifications(offset: 0, limit: 200, query: "") {
      ...NotificationFragment
    }
  }
  ${E}
`,Ge=`
  query clipboard($offset: Int!, $limit: Int!, $query: String!) {
    clipboard(offset: $offset, limit: $limit, query: $query) {
      ...ClipboardFragment
    }
    clipboardCount(query: $query)
  }
  ${D}
`,Ke=`
  query {
    deviceInfo {
      ...DeviceInfoFragment
    }
    deviceStatus {
      ...DeviceStatusFragment
    }
  }
  ${te}
  ${O}
`,qe=`
  query {
    deviceStatus {
      batteryLevel
      charging
    }
  }
`,Je=`
  query AppLogs($offset: Int!, $limit: Int!) {
    appLogs(offset: $offset, limit: $limit, query: "")
  }
`,$=`
  query {
    appLogPath
  }
`,Ye=`
  query {
    dbPath
  }
`,Xe=`
  query {
    dataStorePath
  }
`,Ze=`
  query uploadedChunks($fileId: String!) {
    uploadedChunks(fileId: $fileId)
  }
`,Qe=`
  query mergeStatus($fileId: String!) {
    mergeStatus(fileId: $fileId) {
      status
      value
      mergedSize
      error
    }
  }
`,$e=`
  query {
    pomodoroToday {
      date
      completedCount
      currentRound
      timeLeftSec
      totalTimeSec
      isRunning
      isPaused
      state
    }
    pomodoroSettings {
      workDurationMin
      shortBreakDurationMin
      longBreakDurationMin
      pomodorosBeforeLongBreak
      showNotification
      playSoundOnComplete
    }
  }
`,et=`
  query {
    dataStoreEntries {
      key
      value
    }
  }
`,tt=`
  query {
    dbTables
  }
`,nt=`
  query DbTableRowCount($table: String!) {
    dbTableRowCount(table: $table)
  }
`,rt=`
  query DbTableRows($table: String!, $offset: Int!, $limit: Int!) {
    dbTableRows(table: $table, offset: $offset, limit: $limit)
  }
`,it=`
  query DbTableInfo($table: String!) {
    dbTableInfo(table: $table) {
      idKey
    }
  }
`,at=`
  query {
    bookmarks {
      ...BookmarkFragment
    }
    bookmarkGroups {
      ...BookmarkGroupFragment
    }
  }
  ${k}
  ${A}
`,ot=`
  query {
    isDiscovering
  }
`;export{de as $,ue as A,D as At,se as B,Ye as C,ke as Ct,tt as D,A as Dt,rt as E,k as Et,je as F,x as Ft,F as G,Te as H,we as I,E as It,Be as J,P as K,be as L,c as Lt,De as M,w as Mt,pe as N,C as Nt,Ke as O,j as Ot,xe as P,_ as Pt,le as Q,Se as R,Xe as S,Ze as St,nt as T,u as Tt,re as U,X as V,ne as W,_e as X,z as Y,Qe as Z,Ge as _,q as _t,Fe as a,ze as at,J as b,V as bt,he as c,$e as ct,Ce as d,fe as dt,Le as et,Ne as f,ie as ft,I as g,K as gt,L as h,G as ht,Je as i,Ae as it,Oe as j,g as jt,qe as k,f as kt,oe as l,ce as lt,H as m,Ve as mt,me as n,ve as nt,Ie as o,Re as ot,Q as p,He as pt,ot as q,$ as r,We as rt,Ee as s,R as st,B as t,ye as tt,at as u,Ue as ut,Me as v,Pe as vt,it as w,ae as wt,et as x,ge as xt,Z as y,W as yt,U as z};