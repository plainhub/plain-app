import{S as e,b as t,n}from"./gql-client-BwDrWtSW.js";var r=`
  query {
    imageEditorProjects {
      id
      thumbnail
      canvasWidth
      canvasHeight
      layerCount
      updatedAt
    }
  }
`,i=`
  query imageEditorProject($id: ID!) {
    imageEditorProject(id: $id) {
      id
      stateB64
      thumbnail
      canvasWidth
      canvasHeight
      layerCount
      updatedAt
    }
  }
`,a=`
  mutation saveImageEditorProject($id: ID!, $input: ImageEditorProjectInput!) {
    saveImageEditorProject(id: $id, input: $input) {
      id
    }
  }
`,o=`
  mutation deleteImageEditorProject($id: ID!) {
    deleteImageEditorProject(id: $id)
  }
`;function s(e){let t=Date.parse(e);return Number.isNaN(t)?Date.now():t}function c(e){return{id:e.id,updatedAt:s(e.updatedAt),canvasWidth:e.canvasWidth,canvasHeight:e.canvasHeight,layerCount:e.layerCount,previewDataUrl:e.thumbnail}}var l=class{async save(e,r,i){let o=await n(a,{id:e,input:{stateB64:t(r.state.buffer),thumbnail:r.thumbnail,canvasWidth:i.canvasWidth,canvasHeight:i.canvasHeight,layerCount:i.layerCount}});if(o.errors?.length)throw Error(o.errors[0].message)}async load(t){let r=await n(i,{id:t});if(r.errors?.length)throw Error(r.errors[0].message);let a=r.data?.imageEditorProject;return a?{state:new Uint8Array(e(a.stateB64)),thumbnail:a.thumbnail}:null}async delete(e){let t=await n(o,{id:e});if(t.errors?.length)throw Error(t.errors[0].message)}async list(){let e=await n(r);return e.errors?.length?[]:(e.data?.imageEditorProjects??[]).map(c)}};export{l as t};