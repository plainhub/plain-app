import{C as e}from"./reactivity.esm-bundler-B13GM2Wg.js";import{Ht as t,L as n,nt as r,ot as i,tt as a}from"./runtime-core.esm-bundler-DY-H7G2c.js";import{r as o,t as s}from"./gql-client-DStlKFhS.js";var c=`
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
    features
    channel
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

`;function M(e){return e instanceof s?e.status===403?`desktop_access_disabled`:e.message:`network_error`}function N(e){if(e)return typeof e==`function`?e():e}function P(s){let c=e(!1),l=e();async function u(e){if(!(s.enabled&&!s.enabled())){c.value=!0;try{let t=e??N(s.variables),n=await o(typeof s.document==`function`?s.document():s.document,t);n.errors?.length?s.handle(n.data,n.errors[0].message):(l.value=n.data,s.handle(n.data,``))}catch(e){s.handle(void 0,M(e))}finally{c.value=!1}}}u();let d=!0;return n()&&(i(()=>{d=!1}),r(()=>{d=!0})),typeof s.variables==`function`&&t(s.variables,async()=>{await a(),d&&u()},{deep:!0}),typeof s.document==`function`&&t(s.document,async()=>{await a(),d&&u()}),s.enabled&&t(s.enabled,async(e,t)=>{await a(),d&&e&&!t&&u()}),{loading:c,result:l,refetch:u}}function F(t){let n=e(!1),r=e(),i=0,a=0,s=0,c;async function l(e,l={}){let u=++i,d=l.latest?++a:void 0,f=e??N(t.variables),p=f?{...f}:void 0,m={variables:p,requestId:u,meta:l.meta};l.latest?c=u:s++,n.value=!0;try{let e=typeof t.document==`function`?t.document():t.document,n=l.force?await o(e,p,{fresh:!0}):await o(e,p);if(l.latest&&d!==a)return;n.errors?.length?t.handle(n.data,n.errors[0].message,m):(r.value=n.data,t.handle(n.data,``,m))}catch(e){(!l.latest||d===a)&&t.handle(void 0,M(e),m)}finally{l.latest?c===u&&(c=void 0):s--,n.value=s>0||c!==void 0}}return{loading:n,result:r,fetch:l}}var I=`
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
    appFileCount
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
    favoriteFolders {
      rootPath
      fullPath
      alias
    }
  }
`,de=`
  query {
    app {
      ...AppFragment
    }
  }
  ${d}
`,fe=`
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
`,pe=`
  query tags($type: DataType!) {
    tags(type: $type) {
      ...TagFragment
    }
  }
  ${c}
`,me=`
  query mediaBuckets($type: MediaDataType!) {
    mediaBuckets(type: $type) {
      id
      name
      itemCount
      topItemPaths
    }
  }
`,he=`
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
`,ge=`
  query note($id: ID!) {
    note(id: $id) {
      ...NoteFragment
    }
  }
  ${x}
`,_e=`
  query {
    feeds {
      ...FeedFragment
    }
  }
  ${C}
`,ve=`
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
`,ye=`
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
`,be=`
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
`,xe=`
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
`,Se=`
  query imageCount($query: String!) {
    total: imageCount(query: $query)
    trash: imageCount(query: "trash:true")
  }
`,Ce=`
  query audioCount($query: String!) {
    total: audioCount(query: $query)
    trash: audioCount(query: "trash:true")
  }
`,we=`
  query docs($offset: Int!, $limit: Int!, $query: String!, $sortBy: FileSortBy!) {
    items: docs(offset: $offset, limit: $limit, query: $query, sortBy: $sortBy) {
      ...DocFragment
    }
    total: docCount(query: $query)
  }
  ${S}
`,Te=`
  query docCount($query: String!) {
    total: docCount(query: $query)
    trash: docCount(query: "trash:true")
    extGroups: docExtGroups {
      ext
      count
    }
  }
`,Ee=`
  query videoCount($query: String!) {
    total: videoCount(query: $query)
    trash: videoCount(query: "trash:true")
  }
`,De=`
  query {
    total: packageCount(query: "")
    system: packageCount(query: "type:SYSTEM")
  }
`,Oe=`
  query {
    total: feedEntryCount(query: "")
    today: feedEntryCount(query: "today:true")
    feedEntryCounts {
      id
      count
    }
  }
`,ke=`
  query {
    total: contactCount(query: "")
  }
`,Ae=`
  query {
    total: callCount(query: "")
    incoming: callCount(query: "type:1")
    outgoing: callCount(query: "type:2")
    missed: callCount(query: "type:3")
  }
`,je=`
  query {
    smsBoxCounts {
      total
      inbox
      sent
      drafts
    }
  }
`,Me=`
  query {
    archivedConversations(offset: 0, limit: 200, query: "") {
      ...SmsConversationFragment
    }
  }
  ${m}
`,Ne=`
  query {
    archivedConversations(offset: 0, limit: 200, query: "") {
      ...SmsConversationWithAddressesFragment
    }
  }
  ${h}
`,Pe=`
  query {
    total: noteCount(query: "")
    trash: noteCount(query: "trash:true")
  }
`,Fe=`
  query packages($offset: Int!, $limit: Int!, $query: String!, $sortBy: FileSortBy!) {
    packages(offset: $offset, limit: $limit, query: $query, sortBy: $sortBy) {
      ...PackageFragment
    }
    packageCount(query: $query)
  }
  ${T}
`,Ie=`
  query packageStatuses($ids: [ID!]!) {
    packageStatuses(ids: $ids) {
      id
      exists
      updatedAt
    }
  }
`,Le=`
  query {
    screenMirrorState
    screenMirrorControlEnabled
    screenMirrorQuality {
      mode
      resolution
    }
  }
`,Re=`
  query {
    screenMirrorVideoCodec {
      annexB
      keyFrame
    }
  }
`,ze=`
  query {
    screenMirrorControlEnabled
  }
`,Be=`
  mutation {
    requestScreenMirrorKeyFrame
  }
`;`${E}`,`${D}`;var Ve=`
  query {
    deviceInfo {
      ...DeviceInfoFragment
    }
    deviceStatus {
      ...DeviceStatusFragment
    }
    sims {
      id
      label
      number
      subscriptionId
    }
  }
  ${te}
  ${O}
`,$=`
  query {
    deviceStatus {
      batteryLevel
      charging
    }
  }
`,He=`
  query AppLogs($offset: Int!, $limit: Int!) {
    appLogs(offset: $offset, limit: $limit, query: "")
  }
`,Ue=`
  query {
    appLogPath
  }
`,We=`
  query {
    dbPath
  }
`,Ge=`
  query {
    dataStorePath
  }
`,Ke=`
  query uploadedChunks($fileId: String!) {
    uploadedChunks(fileId: $fileId)
  }
`,qe=`
  query mergeStatus($fileId: String!) {
    mergeStatus(fileId: $fileId) {
      status
      value
      mergedSize
      error
    }
  }
`,Je=`
  query {
    dataStoreEntries {
      key
      value
    }
  }
`,Ye=`
  query {
    dbTables
  }
`,Xe=`
  query DbTableRowCount($table: String!) {
    dbTableRowCount(table: $table)
  }
`,Ze=`
  query DbTableRows($table: String!, $offset: Int!, $limit: Int!) {
    dbTableRows(table: $table, offset: $offset, limit: $limit)
  }
`,Qe=`
  query DbTableInfo($table: String!) {
    dbTableInfo(table: $table) {
      idKey
    }
  }
`,$e=`
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
`,et=`
  query {
    isDiscovering
  }
`;export{De as $,we as A,E as At,Se as B,Qe as C,f as Ct,Ve as D,C as Dt,Ye as E,w as Et,_e as F,et as G,ne as H,ye as I,qe as J,z as K,U as L,ve as M,Oe as N,$ as O,_ as Ot,xe as P,he as Q,se as R,We as S,j as St,Ze as T,g as Tt,F as U,re as V,P as W,Pe as X,le as Y,ge as Z,ke as _,Ee as _t,Me as a,ie as at,Je as b,k as bt,fe as c,Re as ct,be as d,q as dt,Ie as et,Ae as f,je as ft,I as g,Ke as gt,L as h,pe as ht,He as i,Be as it,ue as j,c as jt,Te as k,x as kt,oe as l,G as lt,H as m,V as mt,de as n,R as nt,Ne as o,ze as ot,Q as p,W as pt,me as q,Ue as r,ce as rt,Ce as s,Le as st,B as t,Fe as tt,$e as u,K as ut,Z as v,ae as vt,Xe as w,D as wt,Ge as x,A as xt,J as y,u as yt,X as z};