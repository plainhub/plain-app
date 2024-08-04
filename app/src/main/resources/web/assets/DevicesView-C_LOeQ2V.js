import{d as y,g as A,h as N,l as F,C as I,ac as r,cw as p,c as o,p as m,a as e,t as a,O as _,P as M,S as q,o as i,w as v,x as h,m as d,ap as f,V as $,U as D,Z as w,ad as B,j as E,a3 as S}from"./index-R8dLcqG_.js";import{_ as T}from"./Breadcrumb-CXy0PDtB.js";import{E as O}from"./EditValueModal-O1aAFYgx.js";import"./vee-validate.esm-vpr6zxQb.js";const U={class:"table-responsive"},j={class:"table"},L=e("th",null,"ID",-1),P={class:"actions one"},Q=["onClick"],Z={class:"nowrap"},z={class:"nowrap"},G={class:"actions one"},H=["onClick"],Y=y({__name:"DevicesView",setup(J){const{t:s}=A(),c=N([]);F({handle:(t,l)=>{l?I(s(l),"error"):c.value=[...t.devices]},document:r`
    query {
      devices {
        ...DeviceFragment
      }
    }
    ${p}
  `});function g(t){w(B,{id:t.id,name:t.name||s("unknown"),gql:r`
      mutation DeleteDevice($id: ID!) {
        deleteDevice(id: $id)
      }
    `,appApi:!1,typeName:"Device"})}function k(t){w(O,{title:s("rename"),placeholder:s("name"),mutation:()=>E({document:r`
          mutation updateDeviceName($id: ID!, $name: String!) {
            updateDeviceName(id: $id, name: $name) {
              ...DeviceFragment
            }
          }
          ${p}
        `,appApi:!1}),value:t.name?t.name:s("unknown"),getVariables:l=>({id:t.id,name:l})})}return(t,l)=>{const V=T,C=S,u=q("tooltip");return i(),o(_,null,[m(V,{current:()=>t.$t("page_title.devices")},null,8,["current"]),e("div",U,[e("table",j,[e("thead",null,[e("tr",null,[L,e("th",null,a(t.$t("name")),1),e("th",null,a(t.$t("ip_address")),1),e("th",null,a(t.$t("mac_address")),1),e("th",null,a(t.$t("manufacturer")),1),e("th",null,a(t.$t("status")),1),e("th",null,a(t.$t("created_at")),1),e("th",null,a(t.$t("active_at")),1),e("th",P,a(t.$t("actions")),1)])]),e("tbody",null,[(i(!0),o(_,null,M(c.value,n=>(i(),o("tr",{key:n.id},[e("td",null,[m(C,{id:n.id,raw:n},null,8,["id","raw"])]),e("td",null,[e("a",{href:"#",onClick:v(b=>k(n),["prevent"])},a(n.name?n.name:t.$t("unknown")),9,Q)]),e("td",null,a(n.ip4),1),e("td",null,a(n.mac.toUpperCase()),1),e("td",null,a(n.macVendor?n.macVendor:t.$t("unknown")),1),e("td",null,a(t.$t(n.isOnline?"online":"offline")),1),e("td",Z,[h((i(),o("time",null,[$(a(d(D)(n.createdAt)),1)])),[[u,d(f)(n.createdAt)]])]),e("td",z,[h((i(),o("time",null,[$(a(d(D)(n.activeAt)),1)])),[[u,d(f)(n.activeAt)]])]),e("td",G,[e("a",{href:"#",class:"v-link",onClick:v(b=>g(n),["prevent"])},a(t.$t("delete")),9,H)])]))),128))])])])],64)}}});export{Y as default};
