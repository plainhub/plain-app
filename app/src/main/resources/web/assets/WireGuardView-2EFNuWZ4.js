import{d as y,u as D,h as M,g as B,l as F,C as q,ab as o,bR as g,j as E,c as r,p as f,a as e,t,O as $,P as N,o as i,m,w as b,f as P,Y as S,ac as V,a2 as j}from"./index-BxNI00MG.js";import{_ as A}from"./Breadcrumb-nQ7aa4HG.js";import{p as L}from"./parser-DNY2aV9Q.js";const O={class:"table-responsive"},Q={class:"table"},R=e("th",null,"ID",-1),Y={class:"actions two"},z={class:"form-check"},H=["disabled","onChange","checked"],J={class:"actions two"},K=["onClick"],T=["onClick"],ee=y({__name:"WireGuardView",setup(U){const w=D(),d=M([]),{t:u}=B();function k(n){P(w,`/wireguard/${n}`)}F({handle:(n,s)=>{s?q(u(s),"error"):d.value=n.wireGuards.map(l=>({...l,...L(l.config)}))},document:o`
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
    `,appApi:!1,typeName:"WireGuard"})}return(n,s)=>{const l=A,I=j;return i(),r($,null,[f(l,{current:()=>n.$t("page_title.wireguard")},null,8,["current"]),e("div",O,[e("table",Q,[e("thead",null,[e("tr",null,[R,e("th",null,t(n.$t("name")),1),e("th",null,t(n.$t("address")),1),e("th",null,t(n.$t("status")),1),e("th",null,t(n.$t("enabled")),1),e("th",Y,t(n.$t("actions")),1)])]),e("tbody",null,[(i(!0),r($,null,N(d.value,a=>{var c,p,_;return i(),r("tr",{key:a.id},[e("td",null,[f(I,{id:a.id,raw:a},null,8,["id","raw"])]),e("td",null,t((c=a.wgInterface)==null?void 0:c.name),1),e("td",null,t((_=(p=a.wgInterface)==null?void 0:p.address)==null?void 0:_.join(", ")),1),e("td",null,t(a.isActive?n.$t("running")+` (${m(u)("listening_port")}: ${a.listeningPort})`:n.$t("stopped")),1),e("td",null,[e("div",z,[e("md-checkbox",{"touch-target":"wrapper",disabled:m(G),onChange:h=>C(a),checked:a.isEnabled},null,40,H)])]),e("td",J,[e("a",{href:"#",class:"v-link",onClick:b(h=>k(a.id),["prevent"])},t(n.$t("edit")),9,K),e("a",{href:"#",class:"v-link",onClick:b(h=>W(a),["prevent"])},t(n.$t("delete")),9,T)])])}),128))])])])],64)}}});export{ee as default};
