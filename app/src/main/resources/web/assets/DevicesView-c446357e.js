import{d as A,u as y,r as C,i as F,k as N,a8 as r,c1 as p,K as I,e as o,f as e,x as _,t as a,F as M,A as T,o as i,w as m,L as v,h as d,T as h,g as f,U as $,Y as D,a9 as q,a2 as B,a0 as E}from"./index-e23e99bf.js";import{_ as L}from"./Breadcrumb-ba8f6aaf.js";import{E as S}from"./EditValueModal-b22371ac.js";import"./vee-validate.esm-36997df0.js";const U={class:"page-container"},K={class:"main"},O={class:"table-responsive"},Q={class:"table"},Y=e("th",null,"ID",-1),j={class:"actions one"},z=["onClick"],G={class:"nowrap"},H={class:"nowrap"},J={class:"actions one"},P=["onClick"],ee=A({__name:"DevicesView",setup(R){const{t:s}=y(),c=C([]);F({handle:(n,l)=>{l?N(s(l),"error"):c.value=[...n.devices]},document:r`
    query {
      devices {
        ...DeviceFragment
      }
    }
    ${p}
  `});function g(n){D(q,{id:n.id,name:n.name||s("unknown"),gql:r`
      mutation DeleteDevice($id: ID!) {
        deleteDevice(id: $id)
      }
    `,appApi:!1,typeName:"Device"})}function k(n){D(S,{title:s("rename"),placeholder:s("name"),mutation:()=>B({document:r`
          mutation updateDeviceName($id: ID!, $name: String!) {
            updateDeviceName(id: $id, name: $name) {
              ...DeviceFragment
            }
          }
          ${p}
        `,appApi:!1}),value:n.name?n.name:s("unknown"),getVariables:l=>({id:n.id,name:l})})}return(n,l)=>{const w=L,V=E,u=I("tooltip");return i(),o("div",U,[e("div",K,[_(w,{current:()=>n.$t("page_title.devices")},null,8,["current"]),e("div",O,[e("table",Q,[e("thead",null,[e("tr",null,[Y,e("th",null,a(n.$t("name")),1),e("th",null,a(n.$t("ip_address")),1),e("th",null,a(n.$t("mac_address")),1),e("th",null,a(n.$t("manufacturer")),1),e("th",null,a(n.$t("status")),1),e("th",null,a(n.$t("created_at")),1),e("th",null,a(n.$t("active_at")),1),e("th",j,a(n.$t("actions")),1)])]),e("tbody",null,[(i(!0),o(M,null,T(c.value,t=>(i(),o("tr",{key:t.id},[e("td",null,[_(V,{id:t.id,raw:t},null,8,["id","raw"])]),e("td",null,[e("a",{href:"#",onClick:m(b=>k(t),["prevent"])},a(t.name?t.name:n.$t("unknown")),9,z)]),e("td",null,a(t.ip4),1),e("td",null,a(t.mac.toUpperCase()),1),e("td",null,a(t.macVendor?t.macVendor:n.$t("unknown")),1),e("td",null,a(n.$t(t.isOnline?"online":"offline")),1),e("td",G,[v((i(),o("span",null,[f(a(d($)(t.createdAt)),1)])),[[u,d(h)(t.createdAt)]])]),e("td",H,[v((i(),o("span",null,[f(a(d($)(t.activeAt)),1)])),[[u,d(h)(t.activeAt)]])]),e("td",J,[e("a",{href:"#",class:"v-link",onClick:m(b=>g(t),["prevent"])},a(n.$t("delete")),9,P)])]))),128))])])])])])}}});export{ee as default};
