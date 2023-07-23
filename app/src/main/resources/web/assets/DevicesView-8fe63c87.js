import{_ as w}from"./FieldId-d1a7a4f2.js";import{_ as b}from"./Breadcrumb-2a376aed.js";import{d as V,u as y,r as A,i as C,t as F,Z as i,bV as u,o as r,c as d,b as e,e as m,f as a,F as I,y as N,w as p,g as l,P as _,Q as f,Y as h,V as q}from"./index-9f7eccb0.js";import{_ as B}from"./DeleteConfirm.vuevuetypescriptsetuptruelang-7be37041.js";import{_ as M}from"./EditValueModal.vuevuetypescriptsetuptruelang-de109d01.js";import"./VModal.vuevuetypescriptsetuptruelang-e552d784.js";import"./vee-validate.esm-d2a0b94e.js";const Q={class:"page-container container"},S={class:"main"},T={class:"table"},E=e("th",null,"ID",-1),L={class:"actions one"},O=["onClick"],P=["title"],U=["title"],Y={class:"actions one"},Z=["onClick"],X=V({__name:"DevicesView",setup(j){const{t:s}=y(),c=A([]);C({handle:(n,o)=>{o?F(s(o),"error"):c.value=[...n.devices]},document:i`
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
        `,appApi:!1}),value:n.name?n.name:s("unknown"),getVariables:o=>({id:n.id,name:o})})}return(n,o)=>{const D=b,g=w;return r(),d("div",Q,[e("div",S,[m(D,{current:()=>n.$t("page_title.devices")},null,8,["current"]),e("table",T,[e("thead",null,[e("tr",null,[E,e("th",null,a(n.$t("name")),1),e("th",null,a(n.$t("ip_address")),1),e("th",null,a(n.$t("mac_address")),1),e("th",null,a(n.$t("manufacturer")),1),e("th",null,a(n.$t("status")),1),e("th",null,a(n.$t("created_at")),1),e("th",null,a(n.$t("active_at")),1),e("th",L,a(n.$t("actions")),1)])]),e("tbody",null,[(r(!0),d(I,null,N(c.value,t=>(r(),d("tr",{key:t.id},[e("td",null,[m(g,{id:t.id,raw:t},null,8,["id","raw"])]),e("td",null,[e("a",{href:"#",onClick:p(k=>v(t),["prevent"])},a(t.name?t.name:n.$t("unknown")),9,O)]),e("td",null,a(t.ip4),1),e("td",null,a(t.mac.toUpperCase()),1),e("td",null,a(t.macVendor?t.macVendor:n.$t("unknown")),1),e("td",null,a(n.$t(t.isOnline?"online":"offline")),1),e("td",{class:"nowrap",title:l(_)(t.createdAt)},a(l(f)(t.createdAt)),9,P),e("td",{class:"nowrap",title:l(_)(t.activeAt)},a(l(f)(t.activeAt)),9,U),e("td",Y,[e("a",{href:"#",class:"v-link",onClick:p(k=>$(t),["prevent"])},a(n.$t("delete")),9,Z)])]))),128))])])])])}}});export{X as default};
