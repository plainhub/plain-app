import{d as A,u as N,r as y,i as C,t as F,a7 as r,c3 as p,N as I,o as l,c as i,e,x as _,g as a,F as M,A as q,w as m,O as v,j as d,R as h,f as $,S as f,X as D,a8 as B,a3 as E,$ as S}from"./index-df35a132.js";import{_ as T}from"./Breadcrumb-120bdd32.js";import{E as O}from"./EditValueModal-46b4c4bb.js";import"./vee-validate.esm-aa1410cb.js";const j={class:"page-container"},L={class:"main"},Q={class:"table-responsive"},R={class:"table"},U=e("th",null,"ID",-1),X={class:"actions one"},z=["onClick"],G={class:"nowrap"},H={class:"nowrap"},J={class:"actions one"},K=["onClick"],ee=A({__name:"DevicesView",setup(P){const{t:s}=N(),c=y([]);C({handle:(n,o)=>{o?F(s(o),"error"):c.value=[...n.devices]},document:r`
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
    `,appApi:!1,typeName:"Device"})}function k(n){D(O,{title:s("rename"),placeholder:s("name"),mutation:()=>E({document:r`
          mutation updateDeviceName($id: ID!, $name: String!) {
            updateDeviceName(id: $id, name: $name) {
              ...DeviceFragment
            }
          }
          ${p}
        `,appApi:!1}),value:n.name?n.name:s("unknown"),getVariables:o=>({id:n.id,name:o})})}return(n,o)=>{const w=T,V=S,u=I("tooltip");return l(),i("div",j,[e("div",L,[_(w,{current:()=>n.$t("page_title.devices")},null,8,["current"]),e("div",Q,[e("table",R,[e("thead",null,[e("tr",null,[U,e("th",null,a(n.$t("name")),1),e("th",null,a(n.$t("ip_address")),1),e("th",null,a(n.$t("mac_address")),1),e("th",null,a(n.$t("manufacturer")),1),e("th",null,a(n.$t("status")),1),e("th",null,a(n.$t("created_at")),1),e("th",null,a(n.$t("active_at")),1),e("th",X,a(n.$t("actions")),1)])]),e("tbody",null,[(l(!0),i(M,null,q(c.value,t=>(l(),i("tr",{key:t.id},[e("td",null,[_(V,{id:t.id,raw:t},null,8,["id","raw"])]),e("td",null,[e("a",{href:"#",onClick:m(b=>k(t),["prevent"])},a(t.name?t.name:n.$t("unknown")),9,z)]),e("td",null,a(t.ip4),1),e("td",null,a(t.mac.toUpperCase()),1),e("td",null,a(t.macVendor?t.macVendor:n.$t("unknown")),1),e("td",null,a(n.$t(t.isOnline?"online":"offline")),1),e("td",G,[v((l(),i("span",null,[$(a(d(f)(t.createdAt)),1)])),[[u,d(h)(t.createdAt)]])]),e("td",H,[v((l(),i("span",null,[$(a(d(f)(t.activeAt)),1)])),[[u,d(h)(t.activeAt)]])]),e("td",J,[e("a",{href:"#",class:"v-link",onClick:m(b=>g(t),["prevent"])},a(n.$t("delete")),9,K)])]))),128))])])])])])}}});export{ee as default};
