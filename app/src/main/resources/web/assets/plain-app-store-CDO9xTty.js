import{n as e,x as t,y as n}from"./gql-client-tchz4uZe.js";var r=`
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
`;function s(e){let t=Date.parse(e);return Number.isNaN(t)?Date.now():t}function c(e){return{id:e.id,updatedAt:s(e.updatedAt),canvasWidth:e.canvasWidth,canvasHeight:e.canvasHeight,layerCount:e.layerCount,previewDataUrl:e.thumbnail}}var l=class{async save(t,r,i){let o=await e(a,{id:t,input:{stateB64:n(r.state.buffer),thumbnail:r.thumbnail,canvasWidth:i.canvasWidth,canvasHeight:i.canvasHeight,layerCount:i.layerCount}});if(o.errors?.length)throw Error(o.errors[0].message)}async load(n){let r=await e(i,{id:n});if(r.errors?.length)throw Error(r.errors[0].message);let a=r.data?.imageEditorProject;return a?{state:new Uint8Array(t(a.stateB64)),thumbnail:a.thumbnail}:null}async delete(t){let n=await e(o,{id:t});if(n.errors?.length)throw Error(n.errors[0].message)}async list(){let t=await e(r);return t.errors?.length?[]:(t.data?.imageEditorProjects??[]).map(c)}};export{l as t};