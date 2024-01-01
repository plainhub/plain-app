import{d as y,u as A,r as C,h as F,y as N,ac as r,c2 as p,S as I,c as o,a as e,q as m,t,F as q,K as M,o as i,m as _,n as v,k as d,Z as h,j as $,$ as f,a3 as D,ad as B,i as E,a6 as S}from"./index-2583f876.js";import{_ as T}from"./Breadcrumb-ef080890.js";import{E as j}from"./EditValueModal-13c70216.js";import"./vee-validate.esm-b0601c3b.js";const K={class:"page-container"},L={class:"main"},O={class:"table-responsive"},Q={class:"table"},U=e("th",null,"ID",-1),Z={class:"actions one"},z=["onClick"],G={class:"nowrap"},H={class:"nowrap"},J={class:"actions one"},P=["onClick"],ee=y({__name:"DevicesView",setup(R){const{t:s}=A(),c=C([]);F({handle:(n,l)=>{l?N(s(l),"error"):c.value=[...n.devices]},document:r`
    query {
      devices {
        ...DeviceFragment
      }
    }
    ${p}
  `});function k(n){D(B,{id:n.id,name:n.name||s("unknown"),gql:r`
      mutation DeleteDevice($id: ID!) {
        deleteDevice(id: $id)
      }
    `,appApi:!1,typeName:"Device"})}function g(n){D(j,{title:s("rename"),placeholder:s("name"),mutation:()=>E({document:r`
          mutation updateDeviceName($id: ID!, $name: String!) {
            updateDeviceName(id: $id, name: $name) {
              ...DeviceFragment
            }
          }
          ${p}
        `,appApi:!1}),value:n.name?n.name:s("unknown"),getVariables:l=>({id:n.id,name:l})})}return(n,l)=>{const w=T,V=S,u=I("tooltip");return i(),o("div",K,[e("div",L,[m(w,{current:()=>n.$t("page_title.devices")},null,8,["current"]),e("div",O,[e("table",Q,[e("thead",null,[e("tr",null,[U,e("th",null,t(n.$t("name")),1),e("th",null,t(n.$t("ip_address")),1),e("th",null,t(n.$t("mac_address")),1),e("th",null,t(n.$t("manufacturer")),1),e("th",null,t(n.$t("status")),1),e("th",null,t(n.$t("created_at")),1),e("th",null,t(n.$t("active_at")),1),e("th",Z,t(n.$t("actions")),1)])]),e("tbody",null,[(i(!0),o(q,null,M(c.value,a=>(i(),o("tr",{key:a.id},[e("td",null,[m(V,{id:a.id,raw:a},null,8,["id","raw"])]),e("td",null,[e("a",{href:"#",onClick:_(b=>g(a),["prevent"])},t(a.name?a.name:n.$t("unknown")),9,z)]),e("td",null,t(a.ip4),1),e("td",null,t(a.mac.toUpperCase()),1),e("td",null,t(a.macVendor?a.macVendor:n.$t("unknown")),1),e("td",null,t(n.$t(a.isOnline?"online":"offline")),1),e("td",G,[v((i(),o("span",null,[$(t(d(f)(a.createdAt)),1)])),[[u,d(h)(a.createdAt)]])]),e("td",H,[v((i(),o("span",null,[$(t(d(f)(a.activeAt)),1)])),[[u,d(h)(a.activeAt)]])]),e("td",J,[e("a",{href:"#",class:"v-link",onClick:_(b=>k(a),["prevent"])},t(n.$t("delete")),9,P)])]))),128))])])])])])}}});export{ee as default};
