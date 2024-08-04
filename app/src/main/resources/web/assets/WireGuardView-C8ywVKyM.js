import{d as y,u as D,h as M,g as B,l as F,C as q,ac as o,c2 as g,j as E,c as r,p as f,a as e,t,O as $,P as N,o as i,m,w as b,f as P,Z as S,ad as V,a3 as j}from"./index-R8dLcqG_.js";import{_ as A}from"./Breadcrumb-CXy0PDtB.js";import{p as L}from"./parser-DNY2aV9Q.js";const O={class:"table-responsive"},Q={class:"table"},Z=e("th",null,"ID",-1),z={class:"actions two"},H={class:"form-check"},J=["disabled","onChange","checked"],K={class:"actions two"},R=["onClick"],T=["onClick"],ee=y({__name:"WireGuardView",setup(U){const w=D(),d=M([]),{t:u}=B();function k(n){P(w,`/wireguard/${n}`)}F({handle:(n,s)=>{s?q(u(s),"error"):d.value=n.wireGuards.map(l=>({...l,...L(l.config)}))},document:o`
    query {
      wireGuards {
        ...WireGuardFragment
      }
    }
    ${g}
  `});const{mutate:v,loading:G}=E({document:o`
    mutation enableWireGuard($id: ID!, $enable: Boolean!) {
      enableWireGuard(id: $id, enable: $enable) {
        ...WireGuardFragment
      }
    }
    ${g}
  `});function C(n){v({id:n.id,enable:n.isEnabled})}function W(n){var s;S(V,{id:n.id,name:(s=n.wgInterface)==null?void 0:s.name,gql:o`
      mutation DeleteWireGuard($id: ID!) {
        deleteWireGuard(id: $id)
      }
    `,appApi:!1,typeName:"WireGuard"})}return(n,s)=>{const l=A,I=j;return i(),r($,null,[f(l,{current:()=>n.$t("page_title.wireguard")},null,8,["current"]),e("div",O,[e("table",Q,[e("thead",null,[e("tr",null,[Z,e("th",null,t(n.$t("name")),1),e("th",null,t(n.$t("address")),1),e("th",null,t(n.$t("status")),1),e("th",null,t(n.$t("enabled")),1),e("th",z,t(n.$t("actions")),1)])]),e("tbody",null,[(i(!0),r($,null,N(d.value,a=>{var c,p,h;return i(),r("tr",{key:a.id},[e("td",null,[f(I,{id:a.id,raw:a},null,8,["id","raw"])]),e("td",null,t((c=a.wgInterface)==null?void 0:c.name),1),e("td",null,t((h=(p=a.wgInterface)==null?void 0:p.address)==null?void 0:h.join(", ")),1),e("td",null,t(a.isActive?n.$t("running")+` (${m(u)("listening_port")}: ${a.listeningPort})`:n.$t("stopped")),1),e("td",null,[e("div",H,[e("md-checkbox",{"touch-target":"wrapper",disabled:m(G),onChange:_=>C(a),checked:a.isEnabled},null,40,J)])]),e("td",K,[e("a",{href:"#",class:"v-link",onClick:b(_=>k(a.id),["prevent"])},t(n.$t("edit")),9,R),e("a",{href:"#",class:"v-link",onClick:b(_=>W(a),["prevent"])},t(n.$t("delete")),9,T)])])}),128))])])])],64)}}});export{ee as default};
