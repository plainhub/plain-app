import{d as y,u as A,r as C,g as F,x as N,ab as r,c7 as p,R as I,c as o,a as e,p as m,t,F as M,J as q,o as i,l as _,m as v,j as d,Y as h,h as $,Z as f,a2 as D,ac as B,i as E,a5 as T}from"./index-9c78c93b.js";import{_ as S}from"./Breadcrumb-56c0fca0.js";import{E as j}from"./EditValueModal-70202542.js";import"./vee-validate.esm-2d465d6a.js";const J={class:"page-container"},L={class:"main"},O={class:"table-responsive"},Q={class:"table"},R=e("th",null,"ID",-1),U={class:"actions one"},Y=["onClick"],Z={class:"nowrap"},z={class:"nowrap"},G={class:"actions one"},H=["onClick"],ee=y({__name:"DevicesView",setup(K){const{t:s}=A(),c=C([]);F({handle:(n,l)=>{l?N(s(l),"error"):c.value=[...n.devices]},document:r`
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
    `,appApi:!1,typeName:"Device"})}function k(n){D(j,{title:s("rename"),placeholder:s("name"),mutation:()=>E({document:r`
          mutation updateDeviceName($id: ID!, $name: String!) {
            updateDeviceName(id: $id, name: $name) {
              ...DeviceFragment
            }
          }
          ${p}
        `,appApi:!1}),value:n.name?n.name:s("unknown"),getVariables:l=>({id:n.id,name:l})})}return(n,l)=>{const w=S,b=T,u=I("tooltip");return i(),o("div",J,[e("div",L,[m(w,{current:()=>n.$t("page_title.devices")},null,8,["current"]),e("div",O,[e("table",Q,[e("thead",null,[e("tr",null,[R,e("th",null,t(n.$t("name")),1),e("th",null,t(n.$t("ip_address")),1),e("th",null,t(n.$t("mac_address")),1),e("th",null,t(n.$t("manufacturer")),1),e("th",null,t(n.$t("status")),1),e("th",null,t(n.$t("created_at")),1),e("th",null,t(n.$t("active_at")),1),e("th",U,t(n.$t("actions")),1)])]),e("tbody",null,[(i(!0),o(M,null,q(c.value,a=>(i(),o("tr",{key:a.id},[e("td",null,[m(b,{id:a.id,raw:a},null,8,["id","raw"])]),e("td",null,[e("a",{href:"#",onClick:_(V=>k(a),["prevent"])},t(a.name?a.name:n.$t("unknown")),9,Y)]),e("td",null,t(a.ip4),1),e("td",null,t(a.mac.toUpperCase()),1),e("td",null,t(a.macVendor?a.macVendor:n.$t("unknown")),1),e("td",null,t(n.$t(a.isOnline?"online":"offline")),1),e("td",Z,[v((i(),o("span",null,[$(t(d(f)(a.createdAt)),1)])),[[u,d(h)(a.createdAt)]])]),e("td",z,[v((i(),o("span",null,[$(t(d(f)(a.activeAt)),1)])),[[u,d(h)(a.activeAt)]])]),e("td",G,[e("a",{href:"#",class:"v-link",onClick:_(V=>g(a),["prevent"])},t(n.$t("delete")),9,H)])]))),128))])])])])])}}});export{ee as default};
