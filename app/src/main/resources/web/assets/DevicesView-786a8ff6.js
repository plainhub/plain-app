import{_ as w}from"./FieldId-da304ce7.js";import{_ as b}from"./Breadcrumb-23f5606f.js";import{d as y,u as A,r as C,i as F,t as V,Y as i,bh as u,o as r,c as d,a as e,b as m,e as a,F as I,v as N,w as p,j as l,O as _,P as h,X as f,U as q}from"./index-89decb33.js";import{_ as B}from"./DeleteConfirm.vuevuetypescriptsetuptruelang-5ca1a67e.js";import{_ as M}from"./EditValueModal.vuevuetypescriptsetuptruelang-00f692e5.js";import"./VModal.vuevuetypescriptsetuptruelang-6d140002.js";import"./vee-validate.esm-09f61400.js";const O={class:"page-container container"},S={class:"main"},T={class:"table"},U=e("th",null,"ID",-1),j={class:"actions one"},E=["onClick"],L=["title"],P=["title"],Q={class:"actions one"},X=["onClick"],Z=y({__name:"DevicesView",setup(Y){const{t:s}=A(),c=C([]);F({handle:(n,o)=>{o?V(s(o),"error"):c.value=[...n.devices]},document:i`
    query {
      devices {
        ...DeviceFragment
      }
    }
    ${u}
  `});function v(n){f(B,{id:n.id,name:n.name||s("unknown"),gql:i`
      mutation DeleteDevice($id: ID!) {
        deleteDevice(id: $id)
      }
    `,appApi:!1,typeName:"Device"})}function $(n){f(M,{title:s("rename"),placeholder:s("name"),mutation:()=>q({document:i`
          mutation updateDeviceName($id: ID!, $name: String!) {
            updateDeviceName(id: $id, name: $name) {
              ...DeviceFragment
            }
          }
          ${u}
        `,appApi:!1}),value:n.name?n.name:s("unknown"),getVariables:o=>({id:n.id,name:o})})}return(n,o)=>{const D=b,g=w;return r(),d("div",O,[e("div",S,[m(D,{current:()=>n.$t("page_title.devices")},null,8,["current"]),e("table",T,[e("thead",null,[e("tr",null,[U,e("th",null,a(n.$t("name")),1),e("th",null,a(n.$t("ip_address")),1),e("th",null,a(n.$t("mac_address")),1),e("th",null,a(n.$t("manufacturer")),1),e("th",null,a(n.$t("status")),1),e("th",null,a(n.$t("created_at")),1),e("th",null,a(n.$t("active_at")),1),e("th",j,a(n.$t("actions")),1)])]),e("tbody",null,[(r(!0),d(I,null,N(c.value,t=>(r(),d("tr",{key:t.id},[e("td",null,[m(g,{id:t.id,raw:t},null,8,["id","raw"])]),e("td",null,[e("a",{href:"#",onClick:p(k=>$(t),["prevent"])},a(t.name?t.name:n.$t("unknown")),9,E)]),e("td",null,a(t.ip4),1),e("td",null,a(t.mac.toUpperCase()),1),e("td",null,a(t.macVendor?t.macVendor:n.$t("unknown")),1),e("td",null,a(n.$t(t.isOnline?"online":"offline")),1),e("td",{class:"nowrap",title:l(_)(t.createdAt)},a(l(h)(t.createdAt)),9,L),e("td",{class:"nowrap",title:l(_)(t.activeAt)},a(l(h)(t.activeAt)),9,P),e("td",Q,[e("a",{href:"#",class:"v-link",onClick:p(k=>v(t),["prevent"])},a(n.$t("delete")),9,X)])]))),128))])])])])}}});export{Z as default};
