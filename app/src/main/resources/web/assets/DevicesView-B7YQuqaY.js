import{d as y,g as A,h as N,l as F,C as I,ac as r,cn as m,c as o,p,a as e,t as a,O as _,P as M,S as q,o as i,w as v,x as h,m as d,an as f,V as $,U as D,Z as g,ad as B,j as E,a3 as S}from"./index-Dn0O6zoH.js";import{_ as T}from"./Breadcrumb-nTZpptou.js";import{E as O}from"./EditValueModal-Du2st8tU.js";import"./vee-validate.esm-0lx5owW0.js";const U={class:"table-responsive"},j={class:"table"},L=e("th",null,"ID",-1),P={class:"actions one"},Q=["onClick"],Z={class:"nowrap"},z={class:"nowrap"},G={class:"actions one"},H=["onClick"],Y=y({__name:"DevicesView",setup(J){const{t:s}=A(),c=N([]);F({handle:(n,l)=>{l?I(s(l),"error"):c.value=[...n.devices]},document:r`
    query {
      devices {
        ...DeviceFragment
      }
    }
    ${m}
  `});function k(n){g(B,{id:n.id,name:n.name||s("unknown"),gql:r`
      mutation DeleteDevice($id: ID!) {
        deleteDevice(id: $id)
      }
    `,appApi:!1,typeName:"Device"})}function w(n){g(O,{title:s("rename"),placeholder:s("name"),mutation:()=>E({document:r`
          mutation updateDeviceName($id: ID!, $name: String!) {
            updateDeviceName(id: $id, name: $name) {
              ...DeviceFragment
            }
          }
          ${m}
        `,appApi:!1}),value:n.name?n.name:s("unknown"),getVariables:l=>({id:n.id,name:l})})}return(n,l)=>{const V=T,C=S,u=q("tooltip");return i(),o(_,null,[p(V,{current:()=>n.$t("page_title.devices")},null,8,["current"]),e("div",U,[e("table",j,[e("thead",null,[e("tr",null,[L,e("th",null,a(n.$t("name")),1),e("th",null,a(n.$t("ip_address")),1),e("th",null,a(n.$t("mac_address")),1),e("th",null,a(n.$t("manufacturer")),1),e("th",null,a(n.$t("status")),1),e("th",null,a(n.$t("created_at")),1),e("th",null,a(n.$t("active_at")),1),e("th",P,a(n.$t("actions")),1)])]),e("tbody",null,[(i(!0),o(_,null,M(c.value,t=>(i(),o("tr",{key:t.id},[e("td",null,[p(C,{id:t.id,raw:t},null,8,["id","raw"])]),e("td",null,[e("a",{href:"#",onClick:v(b=>w(t),["prevent"])},a(t.name?t.name:n.$t("unknown")),9,Q)]),e("td",null,a(t.ip4),1),e("td",null,a(t.mac.toUpperCase()),1),e("td",null,a(t.macVendor?t.macVendor:n.$t("unknown")),1),e("td",null,a(n.$t(t.isOnline?"online":"offline")),1),e("td",Z,[h((i(),o("time",null,[$(a(d(D)(t.createdAt)),1)])),[[u,d(f)(t.createdAt)]])]),e("td",z,[h((i(),o("time",null,[$(a(d(D)(t.activeAt)),1)])),[[u,d(f)(t.activeAt)]])]),e("td",G,[e("a",{href:"#",class:"v-link",onClick:v(b=>k(t),["prevent"])},a(n.$t("delete")),9,H)])]))),128))])])])],64)}}});export{Y as default};
