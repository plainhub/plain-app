import{Ht as e,L as t,nt as n,ot as r,tt as i,vn as a}from"./chunk-runtime-core.esm-bundler-DrnlzXx3.js";import{r as o,t as s}from"./chunk-gql-client-BvJwaeFl.js";var c=`
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
    logo
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
  fragment ClipboardItemFragment on ClipboardItem {
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
`,fe=`
  query {
    favoriteFolders {
      rootPath
      fullPath
      alias
    }
  }
`,pe=`
  query {
    app {
      ...AppFragment
    }
  }
  ${d}
`,me=`
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
`,he=`
  query tags($type: DataType!) {
    tags(type: $type) {
      ...TagFragment
    }
  }
  ${c}
`,ge=`
  query mediaBuckets($type: MediaDataType!) {
    mediaBuckets(type: $type) {
      id
      name
      itemCount
      topItemPaths
    }
  }
`,_e=`
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
`,ve=`
  query note($id: ID!) {
    note(id: $id) {
      ...NoteFragment
    }
  }
  ${x}
`,ye=`
  query {
    feeds {
      ...FeedFragment
    }
  }
  ${C}
`,be=`
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
`,xe=`
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
`,Se=`
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
`,Ce=`
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
`,we=`
  query imageCount($query: String!) {
    total: imageCount(query: $query)
    trash: imageCount(query: "trash:true")
  }
`,Te=`
  query audioCount($query: String!) {
    total: audioCount(query: $query)
    trash: audioCount(query: "trash:true")
  }
`,Ee=`
  query docs($offset: Int!, $limit: Int!, $query: String!, $sortBy: FileSortBy!) {
    items: docs(offset: $offset, limit: $limit, query: $query, sortBy: $sortBy) {
      ...DocFragment
    }
    total: docCount(query: $query)
  }
  ${S}
`,De=`
  query docCount($query: String!) {
    total: docCount(query: $query)
    trash: docCount(query: "trash:true")
    extGroups: docExtGroups {
      ext
      count
    }
  }
`,Oe=`
  query videoCount($query: String!) {
    total: videoCount(query: $query)
    trash: videoCount(query: "trash:true")
  }
`,ke=`
  query {
    total: packageCount(query: "")
    system: packageCount(query: "type:SYSTEM")
  }
`,Ae=`
  query {
    total: feedEntryCount(query: "")
    today: feedEntryCount(query: "today:true")
    feedEntryCounts {
      id
      count
    }
  }
`,je=`
  query {
    total: contactCount(query: "")
  }
`,Me=`
  query {
    total: callCount(query: "")
    incoming: callCount(query: "type:1")
    outgoing: callCount(query: "type:2")
    missed: callCount(query: "type:3")
  }
`,Ne=`
  query {
    smsBoxCounts {
      total
      inbox
      sent
      drafts
    }
  }
`,Pe=`
  query {
    archivedSmsConversations(offset: 0, limit: 200, query: "") {
      ...SmsConversationFragment
    }
  }
  ${m}
`,Fe=`
  query {
    archivedSmsConversations(offset: 0, limit: 200, query: "") {
      ...SmsConversationWithAddressesFragment
    }
  }
  ${h}
`,Ie=`
  query {
    total: noteCount(query: "")
    trash: noteCount(query: "trash:true")
  }
`,Le=`
  query packages($offset: Int!, $limit: Int!, $query: String!, $sortBy: FileSortBy!) {
    packages(offset: $offset, limit: $limit, query: $query, sortBy: $sortBy) {
      ...PackageFragment
    }
    packageCount(query: $query)
  }
  ${T}
`,Re=`
  query packageStatuses($ids: [ID!]!) {
    packageStatuses(ids: $ids) {
      id
      exists
      updatedAt
    }
  }
`,ze=`
  query {
    isScreenMirroring
    screenMirrorControlEnabled
    screenMirrorQuality {
      mode
      resolution
    }
  }
`,Be=`
  query {
    screenMirrorVideoCodec {
      annexB
      keyFrame
    }
  }
`,Ve=`
  query {
    screenMirrorControlEnabled
  }
`,He=`
  mutation {
    requestScreenMirrorKeyFrame
  }
`,Ue=`
  query {
    notifications(offset: 0, limit: 200, query: "") {
      ...NotificationFragment
    }
  }
  ${E}
`,We=`
  query clipboardItems($offset: Int!, $limit: Int!, $query: String!) {
    clipboardItems(offset: $offset, limit: $limit, query: $query) {
      ...ClipboardItemFragment
    }
    clipboardItemCount(query: $query)
  }
  ${D}
`,Ge=`
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
`,Ke=`
  query {
    deviceStatus {
      batteryLevel
      charging
    }
  }
`,qe=`
  query AppLogs($offset: Int!, $limit: Int!) {
    appLogs(offset: $offset, limit: $limit, query: "")
  }
`,Je=`
  query {
    appLogPath
  }
`,$=`
  query {
    dbPath
  }
`,Ye=`
  query uploadedChunks($fileId: String!) {
    uploadedChunks(fileId: $fileId)
  }
`,Xe=`
  query mergeStatus($fileId: String!) {
    mergeStatus(fileId: $fileId) {
      status
      value
      mergedSize
      error
    }
  }
`,Ze=`
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
`,Qe=`
  query {
    userPrefs
  }
`,$e=`
  query {
    systemPrefs
    userPrefs
  }
`,et=`
  query {
    dbTables
  }
`,tt=`
  query DbTableRowCount($table: String!) {
    dbTableRowCount(table: $table)
  }
`,nt=`
  query DbTableRows($table: String!, $offset: Int!, $limit: Int!) {
    dbTableRows(table: $table, offset: $offset, limit: $limit)
  }
`,rt=`
  query DbTableInfo($table: String!) {
    dbTableInfo(table: $table) {
      idKey
    }
  }
`,it=`
  query DbTableColumns($table: String!) {
    dbTableColumns(table: $table) {
      name
      dataType
      notNull
      defaultValue
      primaryKey
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
`;export{ve as $,De as A,D as At,X as B,rt as C,Oe as Ct,Ge as D,A as Dt,et as E,k as Et,Ce as F,x as Ft,P as G,re as H,ye as I,E as It,z as J,ot as K,xe as L,c as Lt,fe as M,w as Mt,be as N,C as Nt,Ke as O,j as Ot,Ae as P,_ as Pt,Ie as Q,U as R,it as S,Qe as St,nt as T,u as Tt,ne as U,we as V,F as W,Xe as X,ge as Y,le as Z,We as _,Ne as _t,Pe as a,R as at,J as b,he as bt,me as c,ce as ct,Se as d,ie as dt,_e as et,Me as f,Ve as ft,I as g,q as gt,L as h,K as ht,qe as i,Le as it,Ee as j,g as jt,ue as k,f as kt,oe as l,He as lt,H as m,G as mt,pe as n,ke as nt,Fe as o,Ze as ot,Q as p,Be as pt,ze as q,Je as r,Re as rt,Te as s,$e as st,B as t,Ue as tt,at as u,de as ut,je as v,W as vt,tt as w,ae as wt,$ as x,Ye as xt,Z as y,V as yt,se as z};