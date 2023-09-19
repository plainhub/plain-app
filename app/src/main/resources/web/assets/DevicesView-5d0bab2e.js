import{d as A,u as y,r as C,i as F,t as N,a6 as r,c6 as p,L as I,o as l,e as i,f as e,x as _,j as a,F as M,A as q,w as m,M as v,k as d,P as h,g as $,Q as f,V as D,a7 as B,a2 as E,Y as T}from"./index-2c8e7849.js";import{_ as L}from"./Breadcrumb-01ba2071.js";import{E as Q}from"./EditValueModal-66c9db73.js";import"./vee-validate.esm-83a41e33.js";const S={class:"page-container"},j={class:"main"},O={class:"table-responsive"},P={class:"table"},U=e("th",null,"ID",-1),Y={class:"actions one"},z=["onClick"],G={class:"nowrap"},H={class:"nowrap"},J={class:"actions one"},K=["onClick"],ee=A({__name:"DevicesView",setup(R){const{t:s}=y(),c=C([]);F({handle:(n,o)=>{o?N(s(o),"error"):c.value=[...n.devices]},document:r`
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
    `,appApi:!1,typeName:"Device"})}function k(n){D(Q,{title:s("rename"),placeholder:s("name"),mutation:()=>E({document:r`
          mutation updateDeviceName($id: ID!, $name: String!) {
            updateDeviceName(id: $id, name: $name) {
              ...DeviceFragment
            }
          }
          ${p}
        `,appApi:!1}),value:n.name?n.name:s("unknown"),getVariables:o=>({id:n.id,name:o})})}return(n,o)=>{const w=L,V=T,u=I("tooltip");return l(),i("div",S,[e("div",j,[_(w,{current:()=>n.$t("page_title.devices")},null,8,["current"]),e("div",O,[e("table",P,[e("thead",null,[e("tr",null,[U,e("th",null,a(n.$t("name")),1),e("th",null,a(n.$t("ip_address")),1),e("th",null,a(n.$t("mac_address")),1),e("th",null,a(n.$t("manufacturer")),1),e("th",null,a(n.$t("status")),1),e("th",null,a(n.$t("created_at")),1),e("th",null,a(n.$t("active_at")),1),e("th",Y,a(n.$t("actions")),1)])]),e("tbody",null,[(l(!0),i(M,null,q(c.value,t=>(l(),i("tr",{key:t.id},[e("td",null,[_(V,{id:t.id,raw:t},null,8,["id","raw"])]),e("td",null,[e("a",{href:"#",onClick:m(b=>k(t),["prevent"])},a(t.name?t.name:n.$t("unknown")),9,z)]),e("td",null,a(t.ip4),1),e("td",null,a(t.mac.toUpperCase()),1),e("td",null,a(t.macVendor?t.macVendor:n.$t("unknown")),1),e("td",null,a(n.$t(t.isOnline?"online":"offline")),1),e("td",G,[v((l(),i("span",null,[$(a(d(f)(t.createdAt)),1)])),[[u,d(h)(t.createdAt)]])]),e("td",H,[v((l(),i("span",null,[$(a(d(f)(t.activeAt)),1)])),[[u,d(h)(t.activeAt)]])]),e("td",J,[e("a",{href:"#",class:"v-link",onClick:m(b=>g(t),["prevent"])},a(n.$t("delete")),9,K)])]))),128))])])])])])}}});export{ee as default};
