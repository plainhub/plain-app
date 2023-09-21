import{d as A,u as y,r as C,i as F,k as I,a6 as r,c6 as p,I as N,e as o,f as e,x as _,t as a,F as M,A as q,o as i,w as m,J as v,h as d,R as h,g as f,S as $,V as D,a7 as B,a0 as E,Z as S}from"./index-71d8bb76.js";import{_ as T}from"./Breadcrumb-91ff4b3f.js";import{E as J}from"./EditValueModal-7e22b6d3.js";import"./vee-validate.esm-49ea0aba.js";const L={class:"page-container"},O={class:"main"},Q={class:"table-responsive"},R={class:"table"},U=e("th",null,"ID",-1),Z={class:"actions one"},j=["onClick"],z={class:"nowrap"},G={class:"nowrap"},H={class:"actions one"},K=["onClick"],ee=A({__name:"DevicesView",setup(P){const{t:s}=y(),c=C([]);F({handle:(n,l)=>{l?I(s(l),"error"):c.value=[...n.devices]},document:r`
    query {
      devices {
        ...DeviceFragment
      }
    }
    ${p}
  `});function g(n){D(B,{id:n.id,name:n.name||s("unknown"),gql:r`
      mutation DeleteDevice($id: ID!) {
        deleteDevice(id: $id)
      }
    `,appApi:!1,typeName:"Device"})}function k(n){D(J,{title:s("rename"),placeholder:s("name"),mutation:()=>E({document:r`
          mutation updateDeviceName($id: ID!, $name: String!) {
            updateDeviceName(id: $id, name: $name) {
              ...DeviceFragment
            }
          }
          ${p}
        `,appApi:!1}),value:n.name?n.name:s("unknown"),getVariables:l=>({id:n.id,name:l})})}return(n,l)=>{const w=T,V=S,u=N("tooltip");return i(),o("div",L,[e("div",O,[_(w,{current:()=>n.$t("page_title.devices")},null,8,["current"]),e("div",Q,[e("table",R,[e("thead",null,[e("tr",null,[U,e("th",null,a(n.$t("name")),1),e("th",null,a(n.$t("ip_address")),1),e("th",null,a(n.$t("mac_address")),1),e("th",null,a(n.$t("manufacturer")),1),e("th",null,a(n.$t("status")),1),e("th",null,a(n.$t("created_at")),1),e("th",null,a(n.$t("active_at")),1),e("th",Z,a(n.$t("actions")),1)])]),e("tbody",null,[(i(!0),o(M,null,q(c.value,t=>(i(),o("tr",{key:t.id},[e("td",null,[_(V,{id:t.id,raw:t},null,8,["id","raw"])]),e("td",null,[e("a",{href:"#",onClick:m(b=>k(t),["prevent"])},a(t.name?t.name:n.$t("unknown")),9,j)]),e("td",null,a(t.ip4),1),e("td",null,a(t.mac.toUpperCase()),1),e("td",null,a(t.macVendor?t.macVendor:n.$t("unknown")),1),e("td",null,a(n.$t(t.isOnline?"online":"offline")),1),e("td",z,[v((i(),o("span",null,[f(a(d($)(t.createdAt)),1)])),[[u,d(h)(t.createdAt)]])]),e("td",G,[v((i(),o("span",null,[f(a(d($)(t.activeAt)),1)])),[[u,d(h)(t.activeAt)]])]),e("td",H,[e("a",{href:"#",class:"v-link",onClick:m(b=>g(t),["prevent"])},a(n.$t("delete")),9,K)])]))),128))])])])])])}}});export{ee as default};
