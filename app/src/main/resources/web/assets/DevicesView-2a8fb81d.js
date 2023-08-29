import{_ as w}from"./FieldId-7a42c5ae.js";import{_ as b}from"./Breadcrumb-d5bdebe4.js";import{d as A,u as y,r as C,i as F,t as V,a2 as i,bX as u,o as r,c as d,e,f as m,g as a,F as I,A as N,w as p,j as l,S as _,T as f,a1 as h,Z as q}from"./index-5f5c60b6.js";import{_ as B}from"./DeleteConfirm.vuevuetypescriptsetuptruelang-42857978.js";import{_ as M}from"./EditValueModal.vuevuetypescriptsetuptruelang-27a0512e.js";import"./VModal.vuevuetypescriptsetuptruelang-61cd4dae.js";import"./vee-validate.esm-e0a551fe.js";const S={class:"page-container container"},T={class:"main"},j={class:"table"},E=e("th",null,"ID",-1),L={class:"actions one"},O=["onClick"],Q=["title"],U=["title"],X={class:"actions one"},Z=["onClick"],Y=A({__name:"DevicesView",setup(z){const{t:s}=y(),c=C([]);F({handle:(n,o)=>{o?V(s(o),"error"):c.value=[...n.devices]},document:i`
    query {
      devices {
        ...DeviceFragment
      }
    }
    ${u}
  `});function $(n){h(B,{id:n.id,name:n.name||s("unknown"),gql:i`
      mutation DeleteDevice($id: ID!) {
        deleteDevice(id: $id)
      }
    `,appApi:!1,typeName:"Device"})}function v(n){h(M,{title:s("rename"),placeholder:s("name"),mutation:()=>q({document:i`
          mutation updateDeviceName($id: ID!, $name: String!) {
            updateDeviceName(id: $id, name: $name) {
              ...DeviceFragment
            }
          }
          ${u}
        `,appApi:!1}),value:n.name?n.name:s("unknown"),getVariables:o=>({id:n.id,name:o})})}return(n,o)=>{const D=b,g=w;return r(),d("div",S,[e("div",T,[m(D,{current:()=>n.$t("page_title.devices")},null,8,["current"]),e("table",j,[e("thead",null,[e("tr",null,[E,e("th",null,a(n.$t("name")),1),e("th",null,a(n.$t("ip_address")),1),e("th",null,a(n.$t("mac_address")),1),e("th",null,a(n.$t("manufacturer")),1),e("th",null,a(n.$t("status")),1),e("th",null,a(n.$t("created_at")),1),e("th",null,a(n.$t("active_at")),1),e("th",L,a(n.$t("actions")),1)])]),e("tbody",null,[(r(!0),d(I,null,N(c.value,t=>(r(),d("tr",{key:t.id},[e("td",null,[m(g,{id:t.id,raw:t},null,8,["id","raw"])]),e("td",null,[e("a",{href:"#",onClick:p(k=>v(t),["prevent"])},a(t.name?t.name:n.$t("unknown")),9,O)]),e("td",null,a(t.ip4),1),e("td",null,a(t.mac.toUpperCase()),1),e("td",null,a(t.macVendor?t.macVendor:n.$t("unknown")),1),e("td",null,a(n.$t(t.isOnline?"online":"offline")),1),e("td",{class:"nowrap",title:l(_)(t.createdAt)},a(l(f)(t.createdAt)),9,Q),e("td",{class:"nowrap",title:l(_)(t.activeAt)},a(l(f)(t.activeAt)),9,U),e("td",X,[e("a",{href:"#",class:"v-link",onClick:p(k=>$(t),["prevent"])},a(n.$t("delete")),9,Z)])]))),128))])])])])}}});export{Y as default};
