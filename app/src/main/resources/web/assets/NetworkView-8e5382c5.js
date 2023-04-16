import{_ as x}from"./MonacoEditor.vuevuetypescriptsetuptruelang-40a0301f.js";import{_ as y}from"./EditToolbar.vuevuetypescriptsetuptruelang-d8cec4e4.js";import{_ as b}from"./Breadcrumb-55791723.js";import{d as N,u as $,r as s,i as C,t as r,Y as p,b6 as c,U as h,o as U,c as B,b as F,e as i,g as S,J as d,a0 as g}from"./index-3c761cf5.js";const q={class:"page-container container"},D={class:"main"},Q=N({__name:"NetworkView",setup(A){const{t:u}=$(),o=s(0),n=s(""),t=s("");C({handle:(l,e)=>{e?r(u(e),"error"):(n.value=l.networkConfig.netplan,t.value=l.networkConfig.netmix)},document:p`
    query {
      networkConfig {
        ...NetworkConfigFragment
      }
    }
    ${c}
  `});const{mutate:_,loading:f,onDone:v}=h({document:p`
    mutation applyNetplanAndNetmix($netplan: String!, $netmix: String!) {
      applyNetplan(config: $netplan) {
        __typename
      }
      applyNetmix(config: $netmix) {
        ...NetworkConfigFragment
      }
    }
    ${c}
  `});v(()=>{r(u("saved"))});function V(){!n.value||!t.value||_({netplan:n.value,netmix:t.value})}return(l,e)=>{const w=b,k=y,m=x;return U(),B("div",q,[F("div",D,[i(w,{current:()=>l.$t("page_title.network")},null,8,["current"]),i(k,{modelValue:o.value,"onUpdate:modelValue":e[0]||(e[0]=a=>o.value=a),save:V,loading:S(f),tabs:["/etc/netplan/config.yaml","/etc/plainbox/netmix.yaml"]},null,8,["modelValue","loading","tabs"]),d(i(m,{language:"yaml",height:"700",modelValue:n.value,"onUpdate:modelValue":e[1]||(e[1]=a=>n.value=a)},null,8,["modelValue"]),[[g,o.value===0]]),d(i(m,{language:"yaml",height:"700",modelValue:t.value,"onUpdate:modelValue":e[2]||(e[2]=a=>t.value=a)},null,8,["modelValue"]),[[g,o.value===1]])])])}}});export{Q as default};
